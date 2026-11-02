package ru.mari.kechyshot.astro

import kotlin.math.*

/**
 * Offline astronomy (no network, no Android deps — unit-testable on the JVM).
 * Algorithms: J. Meeus, "Astronomical Algorithms" (2nd ed.): Sun ch. 25 (low precision), Moon ch. 47
 * (ELP-2000/82 truncated series; coefficient tables as published by Meeus, transcribed via the MIT-licensed
 * github.com/soniakeys/meeus), illumination ch. 48, rise/set by altitude root search (ch. 15 constants).
 *
 * Moon coefficient tables (TA/TB) transcribed from github.com/soniakeys/meeus (moonposition.go):
 *   Copyright 2013 Sonia Keys. MIT License: Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software ... The above copyright notice and this permission notice shall be included in all copies or
 *   substantial portions of the Software. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 *   (Full text: https://github.com/soniakeys/meeus/blob/master/LICENSE)
 * Typical accuracy: Moon longitude ~10″, Sun ~0.01°, phase instants < 1 min, rise/set < 1 min.
 */
object AstroMath {
    const val RAD = PI / 180.0
    private const val DEG = 180.0 / PI

    fun norm360(x: Double): Double { val r = x % 360.0; return if (r < 0) r + 360.0 else r }
    fun norm180(x: Double): Double { val r = norm360(x); return if (r > 180.0) r - 360.0 else r }

    /** Julian Day (UT) from epoch milliseconds. */
    fun jdFromMillis(ms: Long): Double = ms / 86_400_000.0 + 2440587.5
    fun millisFromJd(jd: Double): Long = Math.round((jd - 2440587.5) * 86_400_000.0)

    /** ΔT = TT − UT in seconds (Espenak & Meeus polynomial, 2005–2050; extrapolated outside). */
    fun deltaT(jdUt: Double): Double { val y = 2000.0 + (jdUt - 2451544.5) / 365.25; val t = y - 2000.0
        return 62.92 + 0.32217 * t + 0.005589 * t * t }
    fun jdTT(jdUt: Double) = jdUt + deltaT(jdUt) / 86400.0

    class Ecl(val lon: Double, val lat: Double, val dist: Double)   // deg, deg, km (Sun: km too)
    class Equ(val ra: Double, val dec: Double, val dist: Double)    // deg, deg, km

    /** Nutation in longitude Δψ (deg) and true obliquity ε (deg), Meeus ch. 22 low-precision. */
    fun nutObl(jde: Double): Pair<Double, Double> {
        val t = (jde - 2451545.0) / 36525.0
        val om = (125.04452 - 1934.136261 * t) * RAD
        val ls = (280.4665 + 36000.7698 * t) * RAD; val lm = (218.3165 + 481267.8813 * t) * RAD
        val dpsi = (-17.20 * sin(om) - 1.32 * sin(2 * ls) - 0.23 * sin(2 * lm) + 0.21 * sin(2 * om)) / 3600.0
        val deps = (9.20 * cos(om) + 0.57 * cos(2 * ls) + 0.10 * cos(2 * lm) - 0.09 * cos(2 * om)) / 3600.0
        val eps0 = 23.0 + 26.0 / 60 + 21.448 / 3600 - (46.8150 * t + 0.00059 * t * t - 0.001813 * t * t * t) / 3600.0
        return dpsi to eps0 + deps
    }

    /** Apparent geocentric ecliptic position of the Sun (Meeus ch. 25), jde = Julian Ephemeris Day. */
    fun sun(jde: Double): Ecl {
        val t = (jde - 2451545.0) / 36525.0
        val l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t * t
        val m = (357.52911 + 35999.05029 * t - 0.0001537 * t * t) * RAD
        val e = 0.016708634 - 0.000042037 * t - 0.0000001267 * t * t
        val c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(m) + (0.019993 - 0.000101 * t) * sin(2 * m) + 0.000289 * sin(3 * m)
        val v = m + c * RAD
        val r = 1.000001018 * (1 - e * e) / (1 + e * cos(v))
        val om = (125.04 - 1934.136 * t) * RAD
        val lon = norm360(l0 + c - 0.00569 - 0.00478 * sin(om))
        return Ecl(lon, 0.0, r * 149_597_870.7)
    }

    private val TA = intArrayOf(
        0, 0, 1, 0, 6288774, -20905355, 2, 0, -1, 0, 1274027, -3699111,
        2, 0, 0, 0, 658314, -2955968, 0, 0, 2, 0, 213618, -569925,
        0, 1, 0, 0, -185116, 48888, 0, 0, 0, 2, -114332, -3149,
        2, 0, -2, 0, 58793, 246158, 2, -1, -1, 0, 57066, -152138,
        2, 0, 1, 0, 53322, -170733, 2, -1, 0, 0, 45758, -204586,
        0, 1, -1, 0, -40923, -129620, 1, 0, 0, 0, -34720, 108743,
        0, 1, 1, 0, -30383, 104755, 2, 0, 0, -2, 15327, 10321,
        0, 0, 1, 2, -12528, 0, 0, 0, 1, -2, 10980, 79661,
        4, 0, -1, 0, 10675, -34782, 0, 0, 3, 0, 10034, -23210,
        4, 0, -2, 0, 8548, -21636, 2, 1, -1, 0, -7888, 24208,
        2, 1, 0, 0, -6766, 30824, 1, 0, -1, 0, -5163, -8379,
        1, 1, 0, 0, 4987, -16675, 2, -1, 1, 0, 4036, -12831,
        2, 0, 2, 0, 3994, -10445, 4, 0, 0, 0, 3861, -11650,
        2, 0, -3, 0, 3665, 14403, 0, 1, -2, 0, -2689, -7003,
        2, 0, -1, 2, -2602, 0, 2, -1, -2, 0, 2390, 10056,
        1, 0, 1, 0, -2348, 6322, 2, -2, 0, 0, 2236, -9884,
        0, 1, 2, 0, -2120, 5751, 0, 2, 0, 0, -2069, 0,
        2, -2, -1, 0, 2048, -4950, 2, 0, 1, -2, -1773, 4130,
        2, 0, 0, 2, -1595, 0, 4, -1, -1, 0, 1215, -3958,
        0, 0, 2, 2, -1110, 0, 3, 0, -1, 0, -892, 3258,
        2, 1, 1, 0, -810, 2616, 4, -1, -2, 0, 759, -1897,
        0, 2, -1, 0, -713, -2117, 2, 2, -1, 0, -700, 2354,
        2, 1, -2, 0, 691, 0, 2, -1, 0, -2, 596, 0,
        4, 0, 1, 0, 549, -1423, 0, 0, 4, 0, 537, -1117,
        4, -1, 0, 0, 520, -1571, 1, 0, -2, 0, -487, -1739,
        2, 1, 0, -2, -399, 0, 0, 0, 2, -2, -381, -4421,
        1, 1, 1, 0, 351, 0, 3, 0, -2, 0, -340, 0,
        4, 0, -3, 0, 330, 0, 2, -1, 2, 0, 327, 0,
        0, 2, 1, 0, -323, 1165, 1, 1, -1, 0, 299, 0,
        2, 0, 3, 0, 294, 0, 2, 0, -1, -2, 0, 8752)
    private val TB = intArrayOf(
        0, 0, 0, 1, 5128122, 0, 0, 1, 1, 280602,
        0, 0, 1, -1, 277693, 2, 0, 0, -1, 173237,
        2, 0, -1, 1, 55413, 2, 0, -1, -1, 46271,
        2, 0, 0, 1, 32573, 0, 0, 2, 1, 17198,
        2, 0, 1, -1, 9266, 0, 0, 2, -1, 8822,
        2, -1, 0, -1, 8216, 2, 0, -2, -1, 4324,
        2, 0, 1, 1, 4200, 2, 1, 0, -1, -3359,
        2, -1, -1, 1, 2463, 2, -1, 0, 1, 2211,
        2, -1, -1, -1, 2065, 0, 1, -1, -1, -1870,
        4, 0, -1, -1, 1828, 0, 1, 0, 1, -1794,
        0, 0, 0, 3, -1749, 0, 1, -1, 1, -1565,
        1, 0, 0, 1, -1491, 0, 1, 1, 1, -1475,
        0, 1, 1, -1, -1410, 0, 1, 0, -1, -1344,
        1, 0, 0, -1, -1335, 0, 0, 3, 1, 1107,
        4, 0, 0, -1, 1021, 4, 0, -1, 1, 833,
        0, 0, 1, -3, 777, 4, 0, -2, 1, 671,
        2, 0, 0, -3, 607, 2, 0, 2, -1, 596,
        2, -1, 1, -1, 491, 2, 0, -2, 1, -451,
        0, 0, 3, -1, 439, 2, 0, 2, 1, 422,
        2, 0, -3, -1, 421, 2, 1, -1, 1, -366,
        2, 1, 0, 1, -351, 4, 0, 0, 1, 331,
        2, -1, 1, 1, 315, 2, -2, 0, -1, 302,
        0, 0, 1, 3, -283, 2, 1, 1, -1, -229,
        1, 1, 0, -1, 223, 1, 1, 0, 1, 223,
        0, 1, -2, -1, -220, 2, 1, -1, -1, -220,
        1, 0, 1, 1, -185, 2, -1, -2, -1, 181,
        0, 1, 2, 1, -177, 4, 0, -2, -1, 176,
        4, -1, -1, -1, 166, 1, 0, 1, -1, -164,
        4, 0, 1, -1, 132, 1, 0, -1, -1, -119,
        4, -1, 0, -1, 115, 2, -2, 0, 1, 107)

    /** Apparent geocentric ecliptic position of the Moon (Meeus ch. 47 + nutation in longitude). */
    fun moon(jde: Double): Ecl {
        val t = (jde - 2451545.0) / 36525.0
        fun h(vararg c: Double): Double { var s = 0.0; for (i in c.indices.reversed()) s = s * t + c[i]; return s }
        val lp = h(218.3164477, 481267.88123421, -0.0015786, 1.0 / 538841, -1.0 / 65194000) * RAD
        val d = h(297.8501921, 445267.1114034, -0.0018819, 1.0 / 545868, -1.0 / 113065000) * RAD
        val m = h(357.5291092, 35999.0502909, -0.0001535, 1.0 / 24490000) * RAD
        val mp = h(134.9633964, 477198.8675055, 0.0087414, 1.0 / 69699, -1.0 / 14712000) * RAD
        val f = h(93.272095, 483202.0175233, -0.0036539, -1.0 / 3526000, 1.0 / 863310000) * RAD
        val a1 = (119.75 + 131.849 * t) * RAD; val a2 = (53.09 + 479264.29 * t) * RAD; val a3 = (313.45 + 481266.484 * t) * RAD
        val e = 1 - 0.002516 * t - 0.0000074 * t * t; val e2 = e * e
        var sl = 3958 * sin(a1) + 1962 * sin(lp - f) + 318 * sin(a2); var sr = 0.0
        var sb = -2235 * sin(lp) + 382 * sin(a3) + 175 * sin(a1 - f) + 175 * sin(a1 + f) + 127 * sin(lp - mp) - 115 * sin(lp + mp)
        var i = 0
        while (i < TA.size) { val mm = TA[i + 1]; val arg = d * TA[i] + m * mm + mp * TA[i + 2] + f * TA[i + 3]
            val k = when (abs(mm)) { 0 -> 1.0; 1 -> e; else -> e2 }
            sl += TA[i + 4] * sin(arg) * k; sr += TA[i + 5] * cos(arg) * k; i += 6 }
        i = 0
        while (i < TB.size) { val mm = TB[i + 1]; val arg = d * TB[i] + m * mm + mp * TB[i + 2] + f * TB[i + 3]
            val k = when (abs(mm)) { 0 -> 1.0; 1 -> e; else -> e2 }
            sb += TB[i + 4] * sin(arg) * k; i += 5 }
        val dpsi = nutObl(jde).first
        return Ecl(norm360(lp * DEG + sl * 1e-6 + dpsi), sb * 1e-6, 385000.56 + sr * 1e-3)
    }

    fun toEqu(ec: Ecl, eps: Double): Equ {
        val l = ec.lon * RAD; val b = ec.lat * RAD; val e = eps * RAD
        val ra = atan2(sin(l) * cos(e) - tan(b) * sin(e), cos(l))
        val dec = asin(sin(b) * cos(e) + cos(b) * sin(e) * sin(l))
        return Equ(norm360(ra * DEG), dec * DEG, ec.dist)
    }

    /** Greenwich apparent sidereal time, degrees. */
    fun gast(jdUt: Double): Double {
        val t = (jdUt - 2451545.0) / 36525.0
        val g = 280.46061837 + 360.98564736629 * (jdUt - 2451545.0) + 0.000387933 * t * t - t * t * t / 38710000.0
        val (dpsi, eps) = nutObl(jdUt)
        return norm360(g + dpsi * cos(eps * RAD))
    }

    enum class Body { SUN, MOON }

    /** Geocentric altitude (deg) of the body's centre, minus the standard rise/set altitude h0. */
    fun altMinusH0(body: Body, jdUt: Double, lat: Double, lon: Double): Double {
        val jde = jdTT(jdUt); val eps = nutObl(jde).second
        val ec = if (body == Body.SUN) sun(jde) else moon(jde)
        val q = toEqu(ec, eps)
        val h0 = if (body == Body.SUN) -0.8333 else 0.7275 * asin(6378.14 / q.dist) * DEG - 0.5667
        val ha = (gast(jdUt) + lon - q.ra) * RAD
        val phi = lat * RAD; val dec = q.dec * RAD
        val alt = asin(sin(phi) * sin(dec) + cos(phi) * cos(dec) * cos(ha)) * DEG
        return alt - h0
    }

    class Crossing(val jd: Double, val rising: Boolean)

    /** All rise (true) / set (false) events of [body] in [jd0, jd1). */
    fun crossings(body: Body, jd0: Double, jd1: Double, lat: Double, lon: Double, stepMin: Double = 10.0): List<Crossing> {
        val out = ArrayList<Crossing>(4); val step = stepMin / 1440.0
        var ta = jd0; var fa = altMinusH0(body, ta, lat, lon)
        while (ta < jd1) {
            val tb = min(ta + step, jd1); val fb = altMinusH0(body, tb, lat, lon)
            if ((fa < 0) != (fb < 0)) {
                var lo = ta; var hi = tb; var flo = fa
                repeat(20) { val mid = (lo + hi) / 2; val fm = altMinusH0(body, mid, lat, lon); if ((fm < 0) == (flo < 0)) { lo = mid; flo = fm } else hi = mid }
                out.add(Crossing((lo + hi) / 2, fa < 0))
            }
            ta = tb; fa = fb
        }
        return out
    }

    /** Moon − Sun apparent ecliptic longitude, 0..360 (0 = new moon, 180 = full). */
    fun elongation(jdUt: Double): Double { val jde = jdTT(jdUt); return norm360(moon(jde).lon - sun(jde).lon) }

    /** Illuminated fraction of the Moon's disk, 0..1 (Meeus ch. 48). */
    fun illumination(jdUt: Double): Double {
        val jde = jdTT(jdUt); val m = moon(jde); val s = sun(jde)
        val psi = acos(cos(m.lat * RAD) * cos((m.lon - s.lon) * RAD))
        val i = atan2(s.dist * sin(psi), m.dist - s.dist * cos(psi))
        return (1 + cos(i)) / 2
    }

    /** Instant (JD UT) nearest to [jdGuess] when elongation equals [target] deg (0/90/180/270). */
    fun solveElongation(target: Double, jdGuess: Double): Double {
        var t = jdGuess
        repeat(8) { val d = norm180(elongation(t) - target); t -= d / 12.190749; if (abs(d) < 1e-6) return t }
        return t
    }

    /** Last new moon at or before [jdUt]. */
    fun prevNewMoon(jdUt: Double): Double {
        var nm = solveElongation(0.0, jdUt - elongation(jdUt) / 12.190749)
        if (nm > jdUt) nm = solveElongation(0.0, nm - 29.530589)
        return nm
    }

    /** Apparent ecliptic longitude of the Moon at UT instant. */
    fun moonLon(jdUt: Double) = moon(jdTT(jdUt)).lon

    /** Instant when the Moon's longitude crosses [target] deg, near [jdGuess]. */
    fun solveMoonLon(target: Double, jdGuess: Double): Double {
        var t = jdGuess
        repeat(8) { val d = norm180(moonLon(t) - target); t -= d / 13.176; if (abs(d) < 1e-6) return t }
        return t
    }
}
