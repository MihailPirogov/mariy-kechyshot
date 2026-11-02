package ru.mari.kechyshot.astro

/**
 * Traditional (folk-astrological) lunar gardening and haircut calendar, expressed as plain rules.
 * This is folk belief, not science; wording is our own and intentionally short and neutral.
 *
 * Rules:
 *  - A day with a new moon or full moon instant: unfavourable for both planting and haircuts.
 *  - Moon sign fertility: fertile = Cancer, Scorpio, Pisces; moderately fertile = Taurus, Libra, Capricorn;
 *    barren = Aries, Gemini, Leo, Virgo, Sagittarius, Aquarius.
 *  - Waxing Moon: crops that yield above ground; waning Moon: root crops and bulbs, pruning, harvest for storage.
 *  - Haircuts: favourable signs Taurus, Leo, Virgo, Capricorn; unfavourable Cancer, Pisces; others neutral.
 *    Waxing Moon — hair is believed to grow back faster; waning — the cut is believed to keep its shape longer.
 */
object Folk {
    enum class Verdict(val ru: String, val mark: String) { GOOD("Благоприятно", "●"), FAIR("Умеренно", "◐"), BAD("Неблагоприятно", "○") }
    data class Advice(val verdict: Verdict, val text: String)

    val FERTILE = setOf(Sign.CANCER, Sign.SCORPIO, Sign.PISCES)
    val MODERATE = setOf(Sign.TAURUS, Sign.LIBRA, Sign.CAPRICORN)
    val HAIR_GOOD = setOf(Sign.TAURUS, Sign.LEO, Sign.VIRGO, Sign.CAPRICORN)
    val HAIR_BAD = setOf(Sign.CANCER, Sign.PISCES)

    private fun syzygy(a: AstroDay) = a.phaseEvent?.value?.takeIf { it == Phase.NEW || it == Phase.FULL }

    fun garden(a: AstroDay): Advice {
        syzygy(a)?.let { return Advice(Verdict.BAD, "${it.ru}: посев и пересадку принято отложить.") }
        val s = a.mainSign
        val what = if (a.waxing) "растущая Луна — для культур с урожаем над землёй (зелень, огурцы, томаты)" else "убывающая Луна — для корнеплодов и луковичных, обрезки, сбора на хранение"
        return when (s) {
            in FERTILE -> Advice(Verdict.GOOD, "Луна в плодородном знаке (${s.ru}); $what.")
            in MODERATE -> Advice(Verdict.FAIR, "Луна в знаке средней плодородности (${s.ru}); $what.")
            else -> Advice(Verdict.BAD, "Луна в бесплодном знаке (${s.ru}): посев не советуют; подходят прополка, рыхление, борьба с вредителями.")
        }
    }

    fun haircut(a: AstroDay): Advice {
        syzygy(a)?.let { return Advice(Verdict.BAD, "${it.ru}: стрижку принято перенести.") }
        val s = a.mainSign
        val phase = if (a.waxing) "на растущей Луне волосы, по поверью, отрастают быстрее" else "на убывающей Луне стрижка, по поверью, дольше держит форму"
        return when (s) {
            in HAIR_GOOD -> Advice(Verdict.GOOD, "Луна в знаке ${s.ru}; $phase.")
            in HAIR_BAD -> Advice(Verdict.BAD, "Луна в знаке ${s.ru}: стрижку обычно не советуют.")
            else -> Advice(Verdict.FAIR, "Луна в знаке ${s.ru} — нейтральный день; $phase.")
        }
    }
}
