package fansirsqi.xposed.sesame.util

/**
 * 列表工具类，提供对列表的常用操作。
 *
 * **迁移说明**:
 * - Kotlin代码推荐直接使用 `listOf()` 或 `mutableListOf()`
 * - Java代码可继续使用 `ListUtil.newArrayList()`，但会有弃用警告
 */
object ListUtil {

    /**
     * 创建一个新的ArrayList实例，并使用提供的元素进行初始化。
     *
     * @param objects 要添加到列表中的元素
     * @return 返回包含所有提供元素的新ArrayList
     *
     * @deprecated 在Kotlin中，请使用 `listOf()` 或 `mutableListOf()` 替代
     */
    @Deprecated(
        message = "Use listOf() or mutableListOf() in Kotlin",
        replaceWith = ReplaceWith("mutableListOf(*objects)")
    )
    @JvmStatic
    fun <T> newArrayList(vararg objects: T): MutableList<T> {
        return if (objects.isNotEmpty()) {
            mutableListOf(*objects)
        } else {
            mutableListOf()
        }
    }
}
