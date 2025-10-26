package fansirsqi.xposed.sesame.entity

/**
 * 表示支付宝版本的实体类，可进行版本比较。
 *
 * @property versionString 原始版本字符串
 */
class AlipayVersion(val versionString: String) : Comparable<AlipayVersion> {
    
    // 版本号数组，用于比较
    private val versionArray: Array<Int>

    init {
        val split = versionString.split(".")
        versionArray = Array(split.size) { i ->
            try {
                split[i].toInt()
            } catch (e: NumberFormatException) {
                Int.MAX_VALUE // 如果解析失败，使用 Int.MAX_VALUE 表示
            }
        }
    }

    /**
     * 实现版本比较逻辑。
     *
     * @param other 需要比较的另一个 AlipayVersion 实例
     * @return 负数表示当前版本小于对比版本，正数表示大于，0 表示相等
     */
    override fun compareTo(other: AlipayVersion): Int {
        val thisLength = versionArray.size
        val thatLength = other.versionArray.size
        val minLength = minOf(thisLength, thatLength)

        // 按版本号逐段比较
        for (i in 0 until minLength) {
            val thisVer = versionArray[i]
            val thatVer = other.versionArray[i]
            if (thisVer != thatVer) {
                return thisVer.compareTo(thatVer)
            }
        }

        // 如果所有对应段都相等，返回长度比较结果
        return thisLength.compareTo(thatLength)
    }

    override fun toString(): String = versionString
}
