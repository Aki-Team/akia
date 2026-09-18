package com.akiteam.akia.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/**
 * 内置的常用 {@link AkiDataType} 集合。
 * <p>
 * 简单类型（String / 数值 / 布尔 / UUID / 数组）的原始值与复杂值相同，
 * 转换是恒等操作；{@link #TAG_CONTAINER} 用于在容器内嵌套另一个容器，
 * {@link #listOf} 用于生成某种元素类型的列表类型。
 */
public final class BuiltInDataTypes {

    private BuiltInDataTypes() {
    }

    /** 字符串。 */
    public static final AkiDataType<String, String> STRING = identity(String.class);

    /** 32 位整数。 */
    public static final AkiDataType<Integer, Integer> INTEGER = identity(Integer.class);

    /** 64 位整数。 */
    public static final AkiDataType<Long, Long> LONG = identity(Long.class);

    /** 双精度浮点数。 */
    public static final AkiDataType<Double, Double> DOUBLE = identity(Double.class);

    /** 布尔值。 */
    public static final AkiDataType<Boolean, Boolean> BOOLEAN = identity(Boolean.class);

    /** UUID（原始值即 UUID 本身，序列化时由实现层转为 NBT 长整型对）。 */
    public static final AkiDataType<UUID, UUID> UUID_TYPE = identity(UUID.class);

    /** 字节数组。 */
    public static final AkiDataType<byte[], byte[]> BYTE_ARRAY = identity(byte[].class);

    /** 整数数组。 */
    public static final AkiDataType<int[], int[]> INTEGER_ARRAY = identity(int[].class);

    /** 嵌套容器：把一个 {@link AkiPersistentContainer} 当作值存进另一个容器。 */
    public static final AkiDataType<CompoundTag, AkiPersistentContainer> TAG_CONTAINER = new AkiDataType<>() {

        @Override
        public Class<CompoundTag> getPrimitiveType() {
            return CompoundTag.class;
        }

        @Override
        public Class<AkiPersistentContainer> getComplexType() {
            return AkiPersistentContainer.class;
        }

        @Override
        public CompoundTag toPrimitive(AkiPersistentContainer complex, AkiAdapterContext context) {
            return complex.toNbt();
        }

        @Override
        public AkiPersistentContainer fromPrimitive(CompoundTag primitive, AkiAdapterContext context) {
            AkiPersistentContainer container = context.newPersistentDataContainer();
            container.fromNbt(primitive);
            return container;
        }
    };

    /**
     * 生成一个"元素类型为 {@code elementType}"的列表类型。
     * <p>
     * 例如 {@code BuiltInDataTypes.listOf(BuiltInDataTypes.INTEGER)} 得到
     * {@code List<Integer>} 的类型；{@code listOf(BuiltInDataTypes.TAG_CONTAINER)}
     * 得到 {@code List<AkiPersistentContainer>}。
     *
     * @param elementType 元素类型
     * @return 列表 {@link AkiDataType}
     * @param <P> 元素原始类型
     * @param <C> 元素复杂类型
     */
    public static <P, C> AkiDataType<List<P>, List<C>> listOf(AkiDataType<P, C> elementType) {
        return new AkiDataType<>() {

            @SuppressWarnings("unchecked")
            @Override
            public Class<List<P>> getPrimitiveType() {
                return (Class<List<P>>) (Class<?>) List.class;
            }

            @SuppressWarnings("unchecked")
            @Override
            public Class<List<C>> getComplexType() {
                return (Class<List<C>>) (Class<?>) List.class;
            }

            @Override
            public List<P> toPrimitive(List<C> complex, AkiAdapterContext context) {
                List<P> out = new ArrayList<>(complex.size());
                for (C value : complex) {
                    out.add(elementType.toPrimitive(value, context));
                }
                return out;
            }

            @Override
            public List<C> fromPrimitive(List<P> primitive, AkiAdapterContext context) {
                List<C> out = new ArrayList<>(primitive.size());
                for (P value : primitive) {
                    out.add(elementType.fromPrimitive(value, context));
                }
                return out;
            }
        };
    }

    /** 构造一个恒等类型：原始值与复杂值相同。 */
    private static <T> AkiDataType<T, T> identity(Class<T> type) {
        return new AkiDataType<>() {
            @Override
            public Class<T> getPrimitiveType() {
                return type;
            }

            @Override
            public Class<T> getComplexType() {
                return type;
            }

            @Override
            public T toPrimitive(T complex, AkiAdapterContext context) {
                return complex;
            }

            @Override
            public T fromPrimitive(T primitive, AkiAdapterContext context) {
                return primitive;
            }
        };
    }
}