package xyz.acrylicstyle.customenchantments.api.enchantment

@Suppress("SpellCheckingInspection", "unused")
enum class EnchantmentLevel(private val level: Int) {
    F(2147483647),
    L(32767),
    M(1000),
    XXXXX(50),
    XXXXIX(49),
    XXXXVIII(48),
    XXXXVII(47),
    XXXXVI(46),
    XXXXV(45),
    XXXXIV(44),
    XXXXIII(43),
    XXXXII(42),
    XXXXI(41),
    XXXX(40),
    XXXIX(39),
    XXXVIII(38),
    XXXVII(37),
    XXXVI(36),
    XXXV(35),
    XXXIV(34),
    XXXIII(33),
    XXXII(32),
    XXXI(31),
    XXX(30),
    XXIX(29),
    XXVIII(28),
    XXVII(27),
    XXVI(26),
    XXV(25),
    XXIV(24),
    XXIII(23),
    XXII(22),
    XXI(21),
    XX(20),
    XIX(19),
    XVIII(18),
    XVII(17),
    XVI(16),
    XV(15),
    XIV(14),
    XIII(13),
    XII(12),
    XI(11),
    X(10),
    IX(9),
    VIII(8),
    VII(7),
    VI(6),
    V(5),
    IV(4),
    III(3),
    II(2),
    I(1),
    ;

    companion object {
        fun getByLevel(level: Int): EnchantmentLevel? = entries.find { enchantment -> enchantment.level == level }

        fun getByName(name: String): EnchantmentLevel? = entries.find { enchantment -> enchantment.name == name }
    }

    override fun toString() = name

    fun getLevel(): Int = level
}
