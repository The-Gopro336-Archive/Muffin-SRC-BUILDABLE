package me.han.muffin.client.utils.combat

data class DamageData(val targetDamage: Float, val selfDamage: Float, val distance: Double) {
    override fun toString(): String {
        return "$targetDamage, $selfDamage, $distance"
    }
}
