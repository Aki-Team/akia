package com.akiteam.akiloader.persistence;

/**
 * PDC 的类型系统（参考 Paper 的 {@code PersistentDataType}）。
 * <p>
 * 定义一种数据在<b>复杂类型</b>（插件看到的形态，如 {@link java.util.UUID}、
 * {@link AkiPersistentContainer}）与<b>原始类型</b>（容器内部实际存储的形态，
 * 如 {@code long[]}、{@code net.minecraft.nbt.CompoundTag}）之间的双向转换。
 * <ul>
 *     <li>{@link #toPrimitive}：把复杂值降为原始值（存库前调用）；</li>
 *     <li>{@link #fromPrimitive}：把原始值升为复杂值（读取时调用）。</li>
 * </ul>
 * 对于 String / Integer 这类简单类型，两个转换都是恒等操作；只有像
 * {@code TAG_CONTAINER}、{@code listOf(...)} 这样的复杂类型才有实际转换逻辑。
 *
 * @param <P> 原始类型（Primitive）
 * @param <C> 复杂类型（Complex）
 */
public interface AkiDataType<P, C> {

    /**
     * 原始类型的 {@link Class} 对象。
     *
     * @return 原始类型
     */
    Class<P> getPrimitiveType();

    /**
     * 复杂类型的 {@link Class} 对象。
     *
     * @return 复杂类型
     */
    Class<C> getComplexType();

    /**
     * 将一个复杂值转为原始值。
     *
     * @param complex 要存储的复杂值
     * @param context 适配上下文（用于需要创建嵌套容器、标签等对象时）
     * @return 原始值
     */
    P toPrimitive(C complex, AkiAdapterContext context);

    /**
     * 将一个原始值还原为复杂值。
     *
     * @param primitive 从存储中读出的原始值
     * @param context   适配上下文
     * @return 复杂值
     */
    C fromPrimitive(P primitive, AkiAdapterContext context);
}