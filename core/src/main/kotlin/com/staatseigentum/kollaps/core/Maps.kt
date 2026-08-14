package com.staatseigentum.kollaps.core

/**
 * `merge`, on every platform.
 *
 * `MutableMap.merge` is not Kotlin's — it is a default method on `java.util.Map`, which Kotlin
 * simply exposes where a JVM is underneath. On a phone that has no JVM the call does not exist,
 * and it does not fail politely either: the compiler cannot resolve the lambda's parameters, so
 * one missing method arrives as four errors about a multiplication it could not type.
 *
 * The behaviour is the same one the standard method has and the same one the four call sites
 * relied on: absent means put the value as it stands, present means join the two. That
 * distinction is load-bearing — the modifiers are multiplied together, and treating an absent
 * entry as zero rather than as the value itself would silence every collector it touched.
 */
internal fun <K> MutableMap<K, Double>.combine(
    key: K,
    value: Double,
    join: (Double, Double) -> Double,
) {
    val existing = this[key]
    this[key] = if (existing == null) value else join(existing, value)
}
