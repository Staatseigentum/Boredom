package com.staatseigentum.kollaps.core

/** Determines how the renderer draws a body. */
enum class BodyKind {
    /** Solid, cratered rock. */
    ROCK,

    /** Rock with a coloured atmosphere. */
    TERRESTRIAL,

    /** Banded gas giant. */
    GAS,

    /** Self-luminous, with corona and flares. */
    STAR,

    /** Tiny, brutally bright, with polar jets. */
    EXOTIC,

    /** Event horizon plus accretion disk. */
    SINGULARITY,
}

/**
 * One step of the progression ladder. The player always taps the tier they have reached; the
 * tier is derived from the mass collected in the current run, so it climbs on its own as the
 * counter grows.
 *
 * Colours are plain ARGB longs so this module stays free of any UI dependency.
 */
data class CelestialTier(
    val index: Int,
    val name: String,
    val flavor: String,
    /** Mass collected in the current run needed to reach this tier. */
    val threshold: Double,
    /** Multiplies every source of mass while this tier is active. */
    val productionMultiplier: Double,
    val kind: BodyKind,
    val primaryColor: Long,
    val secondaryColor: Long,
    val glowColor: Long,
    /** Colour of the surface features — continents, storms. Defaults to [secondaryColor]. */
    val accentColor: Long? = null,
    val hasRing: Boolean = false,
    /**
     * Fraction of the available drawing area the body should fill. Climbs across the ladder so
     * every step is visibly bigger than the last; the neutron star is the one place it drops,
     * because the supergiant collapsing into something small is the point of that step.
     */
    val relativeSize: Float = 0.6f,
) {
    val isFinal: Boolean get() = index == Tiers.all.lastIndex
}

object Tiers {

    val all: List<CelestialTier> = listOf(
        CelestialTier(
            index = 0,
            name = "Meteorit",
            flavor = "Ein Klumpen Gestein, der niemandem gehört. Fang klein an.",
            threshold = 0.0,
            productionMultiplier = 1.0,
            kind = BodyKind.ROCK,
            primaryColor = 0xFF6E6259,
            secondaryColor = 0xFF3A342F,
            glowColor = 0xFF8A7A6A,
            relativeSize = 0.44f,
        ),
        CelestialTier(
            index = 1,
            name = "Asteroid",
            flavor = "Groß genug, um einen Namen und eine Nummer zu bekommen.",
            threshold = 200.0,
            productionMultiplier = 1.35,
            kind = BodyKind.ROCK,
            primaryColor = 0xFF8A8078,
            secondaryColor = 0xFF4A443F,
            glowColor = 0xFF9C9187,
            relativeSize = 0.48f,
        ),
        CelestialTier(
            index = 2,
            name = "Zwergplanet",
            flavor = "Rund genug für Stolz, zu klein für Respekt.",
            threshold = 2_500.0,
            productionMultiplier = 1.9,
            kind = BodyKind.ROCK,
            primaryColor = 0xFFA8A29A,
            secondaryColor = 0xFF5C574F,
            glowColor = 0xFFBDB6AC,
            relativeSize = 0.55f,
        ),
        CelestialTier(
            index = 3,
            name = "Merkur",
            flavor = "Verbrannt, vernarbt, aber offiziell ein Planet.",
            threshold = 50_000.0,
            productionMultiplier = 2.7,
            kind = BodyKind.ROCK,
            primaryColor = 0xFF9C8C7A,
            secondaryColor = 0xFF5A4E42,
            glowColor = 0xFFB09E8A,
            relativeSize = 0.59f,
        ),
        CelestialTier(
            index = 4,
            name = "Mars",
            flavor = "Rost, Staub und ein paar sehr einsame Rover.",
            threshold = 400_000.0,
            productionMultiplier = 3.8,
            kind = BodyKind.TERRESTRIAL,
            primaryColor = 0xFFC1440E,
            secondaryColor = 0xFF7A2B08,
            glowColor = 0xFFFF6B3D,
            relativeSize = 0.63f,
        ),
        CelestialTier(
            index = 5,
            name = "Venus",
            flavor = "Schön aus der Ferne. Aus der Nähe 460 Grad.",
            threshold = 3_000_000.0,
            productionMultiplier = 5.5,
            kind = BodyKind.TERRESTRIAL,
            primaryColor = 0xFFE3B778,
            secondaryColor = 0xFFA8783C,
            glowColor = 0xFFFFD9A0,
            relativeSize = 0.67f,
        ),
        CelestialTier(
            index = 6,
            name = "Erde",
            flavor = "Der einzige Ort mit Kaffee. Behandle ihn gut.",
            threshold = 20_000_000.0,
            productionMultiplier = 8.0,
            kind = BodyKind.TERRESTRIAL,
            primaryColor = 0xFF2E7FD4,
            secondaryColor = 0xFF1B4F86,
            glowColor = 0xFF5FA8FF,
            accentColor = 0xFF3E8F42,
            relativeSize = 0.70f,
        ),
        CelestialTier(
            index = 7,
            name = "Neptun",
            flavor = "Windgeschwindigkeit: 2000 km/h. Niemand beschwert sich.",
            threshold = 150_000_000.0,
            productionMultiplier = 12.0,
            kind = BodyKind.GAS,
            primaryColor = 0xFF3B5FCF,
            secondaryColor = 0xFF21367A,
            glowColor = 0xFF6E8CFF,
            relativeSize = 0.74f,
        ),
        CelestialTier(
            index = 8,
            name = "Uranus",
            flavor = "Liegt auf der Seite und findet das völlig in Ordnung.",
            threshold = 1_000_000_000.0,
            productionMultiplier = 18.0,
            kind = BodyKind.GAS,
            primaryColor = 0xFF7FD8D8,
            secondaryColor = 0xFF3F8C8C,
            glowColor = 0xFFA8ECEC,
            hasRing = true,
            relativeSize = 0.78f,
        ),
        CelestialTier(
            index = 9,
            name = "Saturn",
            flavor = "Der einzige Planet mit richtig gutem Schmuck.",
            threshold = 6_500_000_000.0,
            productionMultiplier = 27.0,
            kind = BodyKind.GAS,
            primaryColor = 0xFFE0C089,
            secondaryColor = 0xFF9A7B4A,
            glowColor = 0xFFF3DCAE,
            hasRing = true,
            relativeSize = 0.81f,
        ),
        CelestialTier(
            index = 10,
            name = "Jupiter",
            flavor = "Ein Sturm, der älter ist als jede Stadt der Erde.",
            threshold = 50_000_000_000.0,
            productionMultiplier = 42.0,
            kind = BodyKind.GAS,
            primaryColor = 0xFFD8A15C,
            secondaryColor = 0xFF8A5C2E,
            glowColor = 0xFFF0C68A,
            relativeSize = 0.85f,
        ),
        CelestialTier(
            index = 11,
            name = "Brauner Zwerg",
            flavor = "Wollte ein Stern werden. Hat es knapp nicht geschafft.",
            threshold = 350_000_000_000.0,
            productionMultiplier = 65.0,
            kind = BodyKind.STAR,
            primaryColor = 0xFF9A5A42,
            secondaryColor = 0xFF3A1F18,
            glowColor = 0xFFB5613F,
            relativeSize = 0.88f,
        ),
        CelestialTier(
            index = 12,
            name = "Roter Zwerg",
            flavor = "Brennt sparsam — und dafür ein paar Billionen Jahre.",
            threshold = 2_500_000_000_000.0,
            productionMultiplier = 100.0,
            kind = BodyKind.STAR,
            primaryColor = 0xFFE05A3A,
            secondaryColor = 0xFF8A2A18,
            glowColor = 0xFFFF7A50,
            relativeSize = 0.91f,
        ),
        CelestialTier(
            index = 13,
            name = "Sonne",
            flavor = "Ganz normaler gelber Zwerg. Für uns trotzdem alles.",
            threshold = 15_000_000_000_000.0,
            productionMultiplier = 160.0,
            kind = BodyKind.STAR,
            primaryColor = 0xFFFFD34D,
            secondaryColor = 0xFFFF9A20,
            glowColor = 0xFFFFE28A,
            relativeSize = 0.94f,
        ),
        CelestialTier(
            index = 14,
            name = "Blauer Riese",
            flavor = "Verschwendet in einer Million Jahren, was andere in Milliarden brauchen.",
            threshold = 100_000_000_000_000.0,
            productionMultiplier = 260.0,
            kind = BodyKind.STAR,
            primaryColor = 0xFF9FD0FF,
            secondaryColor = 0xFF3E7FD0,
            glowColor = 0xFFCFE6FF,
            relativeSize = 0.97f,
        ),
        CelestialTier(
            index = 15,
            name = "Roter Überriese",
            flavor = "So groß, dass die Erdbahn bequem hineinpasst.",
            threshold = 800_000_000_000_000.0,
            productionMultiplier = 420.0,
            kind = BodyKind.STAR,
            primaryColor = 0xFFFF6B4A,
            secondaryColor = 0xFFA02418,
            glowColor = 0xFFFF9A78,
            relativeSize = 1.00f,
        ),
        CelestialTier(
            index = 16,
            name = "Neutronenstern",
            flavor = "Ein Teelöffel davon wiegt so viel wie ein Gebirge.",
            threshold = 5_500_000_000_000_000.0,
            productionMultiplier = 700.0,
            kind = BodyKind.EXOTIC,
            primaryColor = 0xFFE8F4FF,
            secondaryColor = 0xFF86B8E8,
            glowColor = 0xFFBFE4FF,
            relativeSize = 0.56f,
        ),
        CelestialTier(
            index = 17,
            name = "Schwarzes Loch",
            flavor = "Das Ende der Leiter. Ab hier kommt nichts mehr zurück.",
            threshold = 40_000_000_000_000_000.0,
            productionMultiplier = 1_200.0,
            kind = BodyKind.SINGULARITY,
            primaryColor = 0xFF000000,
            secondaryColor = 0xFFFF9A2E,
            glowColor = 0xFFFFB74D,
            relativeSize = 1.00f,
        ),
    )

    val first: CelestialTier get() = all.first()
    val last: CelestialTier get() = all.last()

    /** The tier unlocked by [lifetimeMass] collected during the current run. */
    fun forMass(lifetimeMass: Double): CelestialTier {
        var result = all.first()
        for (tier in all) {
            if (lifetimeMass >= tier.threshold) result = tier else break
        }
        return result
    }

    /** The tier after [tier], or `null` if it is already the black hole. */
    fun next(tier: CelestialTier): CelestialTier? = all.getOrNull(tier.index + 1)

    fun byIndex(index: Int): CelestialTier = all[index.coerceIn(all.indices)]
}
