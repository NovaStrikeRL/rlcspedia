#!/usr/bin/env python3
"""
Genera feed.json para RLCSpedia a partir de LiquipediaDB (API v3).

- Una sola fuente consulta a Liquipedia (GitHub Actions), no cada teléfono.
- Requiere LIQUIPEDIA_API_KEY (secreto del repo). Sin clave, termina sin error.
- Datos de Liquipedia bajo CC-BY-SA 3.0: https://liquipedia.net/rocketleague
Solo usa la biblioteca estándar.
"""
from __future__ import annotations

import gzip
import json
import os
import re
import sys
import time
import urllib.parse
import urllib.request
from datetime import datetime, timedelta, timezone

API = "https://api.liquipedia.net/api/v3"
WIKI = "rocketleague"
WIKI_URL = "https://liquipedia.net/rocketleague/"
UA = "RLCSpedia/0.3 (https://github.com/NovaStrikeRL/rlcspedia)"
LIVE_WINDOW_H = 8          # una serie sin terminar deja de contarse "en vivo" tras 8 h
PAST_DAYS, AHEAD_DAYS = 3, 60


def lpdb(endpoint: str, params: dict, key: str) -> list[dict]:
    q = {"wiki": WIKI, **params}
    req = urllib.request.Request(
        f"{API}/{endpoint}?{urllib.parse.urlencode(q)}",
        headers={"Authorization": f"Apikey {key}", "User-Agent": UA, "Accept-Encoding": "gzip"},
    )
    with urllib.request.urlopen(req, timeout=60) as r:
        raw = r.read()
        if r.headers.get("Content-Encoding") == "gzip":
            raw = gzip.decompress(raw)
    data = json.loads(raw)
    if data.get("error"):
        raise RuntimeError(f"LPDB {endpoint}: {data['error']}")
    time.sleep(2)  # cortesía con el servidor
    return data.get("result", [])


def parse_dt(s: str | None) -> int | None:
    if not s or s.startswith("1970") or s.startswith("0000"):
        return None
    for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%d"):
        try:
            return int(datetime.strptime(s, fmt).replace(tzinfo=timezone.utc).timestamp() * 1000)
        except ValueError:
            pass
    return None


def as_json(v):
    if isinstance(v, str):
        try:
            return json.loads(v)
        except ValueError:
            return {}
    return v or {}


def slug(name: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-")


def short_tag(name: str) -> str:
    words = [w for w in re.split(r"\s+", name) if w.lower() not in {"team", "esports", "gaming", "the"}]
    if not words:
        words = name.split()
    if len(words) == 1:
        return words[0][:4].upper()
    return "".join(w[0] for w in words)[:4].upper()


def region_of(t: dict) -> str:
    loc = as_json(t.get("locations"))
    for k in ("region1", "region", "country1"):
        if isinstance(loc, dict) and loc.get(k):
            return str(loc[k])
    return str(t.get("region") or "")


def is_rlcs(t: dict) -> bool:
    text = " ".join(str(t.get(k) or "") for k in ("name", "tickername", "series", "pagename"))
    return "rlcs" in text.lower() or "championship series" in text.lower()


def build(tournaments: list[dict], matches: list[dict], teams_info: dict[str, dict], now_ms: int) -> dict:
    # Torneos: preferimos RLCS; si no hay ninguno en la ventana, todos los de tier 1.
    rl = [t for t in tournaments if is_rlcs(t)] or [t for t in tournaments if str(t.get("liquipediatier")) == "1"]
    tours = []
    for t in rl:
        start = parse_dt(t.get("startdate")) or 0
        end = (parse_dt(t.get("enddate")) or start) + 24 * 3600 * 1000 - 1
        prize = t.get("prizepool")
        try:
            prize_s = f"${float(prize):,.0f}" if prize not in (None, "", 0, "0") else ""
        except (TypeError, ValueError):
            prize_s = str(prize)
        tours.append({
            "id": t["pagename"],
            "name": t.get("name") or t["pagename"].replace("_", " "),
            "region": region_of(t),
            "tier": {"1": "S-Tier", "2": "A-Tier", "3": "B-Tier"}.get(str(t.get("liquipediatier")), "Tier " + str(t.get("liquipediatier") or "?")),
            "start": start, "end": end, "prize": prize_s,
            "teams": int(t.get("participantsnumber") or 0),
            "venue": {"offline": "LAN", "online": "En línea", "hybrid": "Híbrido"}.get(str(t.get("type") or "").lower(), str(t.get("type") or "")),
            "stage": str(t.get("status") or ""),
            "url": WIKI_URL + urllib.parse.quote(t["pagename"]),
        })
    # Los matches de subpáginas (etapas) tienen parent = "Torneo/Etapa": se asignan por prefijo.
    ids = sorted((x["id"] for x in tours), key=len, reverse=True)

    def owner(parent: str) -> str | None:
        for i in ids:
            if parent == i or parent.startswith(i + "/"):
                return i
        return None

    series, teams = [], {}
    for m in matches:
        tid = owner(m.get("parent") or m.get("pagename") or "")
        ops = m.get("match2opponents") or []
        if not tid or len(ops) < 2 or any(o.get("type") not in (None, "team") for o in ops[:2]):
            continue
        names = [(o.get("name") or "").strip() for o in ops[:2]]
        if not all(names) or any(n.upper() in {"TBD", "BYE"} for n in names):
            continue
        start = parse_dt(m.get("date"))
        if start is None:
            continue
        for o, n in zip(ops[:2], names):
            info = teams_info.get(n, {})
            players = [p.get("displayname") or p.get("name") for p in (o.get("match2players") or []) if p.get("name")]
            tm = teams.setdefault(slug(n), {
                "id": slug(n), "name": n,
                "tag": info.get("shortname") or short_tag(n),
                "region": info.get("region") or "", "players": [],
            })
            for p in players:
                if p and p not in tm["players"]:
                    tm["players"].append(p)

        games = []
        for g in m.get("match2games") or []:
            sc = g.get("scores") or []
            if len(sc) < 2 or sc[0] in (None, "") or sc[1] in (None, ""):
                continue
            try:
                a, b = int(sc[0]), int(sc[1])
            except (TypeError, ValueError):
                continue
            done = str(g.get("winner") or "") in ("1", "2")
            games.append({"scoreA": a, "scoreB": b, "done": done, "overtime": bool(as_json(g.get("extradata")).get("overtime"))})

        def num(v):
            try:
                return max(int(v), 0)
            except (TypeError, ValueError):
                return 0
        wa, wb = num(ops[0].get("score")), num(ops[1].get("score"))
        finished = str(m.get("finished")) == "1"
        if finished:
            status = "DONE"
        elif start <= now_ms and now_ms - start < LIVE_WINDOW_H * 3600 * 1000:
            status = "LIVE"
        elif start > now_ms:
            status = "UPCOMING"
        else:
            continue  # sin terminar y muy vieja: dato incompleto en la wiki
        bo = num(m.get("bestof")) or max(1, 2 * max(wa, wb) - 1)
        series.append({
            "id": m.get("match2id") or f"{tid}-{start}-{slug(names[0])}",
            "tournamentId": tid,
            "stage": (m.get("section") or (m.get("parent") or "").split("/")[-1].replace("_", " ") or "").strip(),
            "bestOf": bo,
            "teamA": slug(names[0]), "teamB": slug(names[1]),
            "start": start, "status": status,
            "winsA": wa, "winsB": wb,
            "games": games,
            "url": WIKI_URL + urllib.parse.quote(m.get("parent") or tid),
        })
    used = {s["tournamentId"] for s in series}
    for t in tours:
        t["hasSeries"] = t["id"] in used
    return {
        "generatedAt": now_ms,
        "source": "Liquipedia",
        "license": "CC-BY-SA 3.0",
        "attribution": "Datos de Liquipedia (liquipedia.net/rocketleague), licencia CC-BY-SA 3.0",
        "tournaments": sorted(tours, key=lambda t: t["start"]),
        "teams": sorted(teams.values(), key=lambda t: t["name"].lower()),
        "series": sorted(series, key=lambda s: s["start"]),
    }


def main() -> int:
    key = os.environ.get("LIQUIPEDIA_API_KEY", "").strip()
    out = sys.argv[1] if len(sys.argv) > 1 else "feed.json"
    if not key:
        print("::warning::Falta el secreto LIQUIPEDIA_API_KEY; no se generó el feed.")
        return 0
    now = datetime.now(timezone.utc)
    d = lambda days: (now + timedelta(days=days)).strftime("%Y-%m-%d")
    tournaments = lpdb("tournament", {
        "conditions": f"[[enddate::>{d(-PAST_DAYS)}]] AND [[startdate::<{d(AHEAD_DAYS)}]] AND ([[liquipediatier::1]] OR [[liquipediatier::2]])",
        "query": "pagename,name,tickername,series,startdate,enddate,liquipediatier,prizepool,participantsnumber,locations,type,status",
        "order": "startdate ASC", "limit": 200,
    }, key)
    matches = lpdb("match", {
        "conditions": f"[[date::>{d(-PAST_DAYS)}]] AND [[date::<{d(AHEAD_DAYS)}]]",
        "query": "match2id,parent,pagename,section,date,dateexact,finished,bestof,winner,match2opponents,match2games",
        "order": "date ASC", "limit": 1000,
    }, key)
    names = sorted({(o.get("name") or "") for m in matches for o in (m.get("match2opponents") or [])[:2] if o.get("name")})
    teams_info: dict[str, dict] = {}
    for i in range(0, len(names), 40):
        chunk = names[i:i + 40]
        cond = " OR ".join(f"[[name::{n}]]" for n in chunk)
        for t in lpdb("team", {"conditions": cond, "query": "name,shortname,region", "limit": 100}, key):
            teams_info[t.get("name")] = t
    feed = build(tournaments, matches, teams_info, int(now.timestamp() * 1000))
    with open(out, "w", encoding="utf-8") as f:
        json.dump(feed, f, ensure_ascii=False, separators=(",", ":"))
    print(f"feed: {len(feed['tournaments'])} torneos, {len(feed['series'])} series, {len(feed['teams'])} equipos")
    return 0


if __name__ == "__main__":
    sys.exit(main())
