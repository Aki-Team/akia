package com.akiteam.akia.game;

import net.minecraft.world.phys.Vec3;

/**
 * 游戏对象：一个生物的只读视图 + 常用操作包装。
 * <p>
 * 继承 {@link Entity}，包装 {@link net.minecraft.world.entity.LivingEntity}，
 * 向插件提供 Paper 开发者熟悉的 API：生命/呼吸/吸收、手持物品、治疗/伤害、
 * 视线与朝向、眼部高度。仅供活体类（玩家/怪物/动物）使用。
 * <p>
 * 服务端线程约束与 null 安全与 {@link Entity} 一致。所有操作经私有助手
 * {@link #living()} 强转为原生活体执行；{@link #getHandle()} 逃生类型仍为
 * 原生存 {@code Entity}。
 */
public class LivingEntity extends Entity {

    private final net.minecraft.world.entity.LivingEntity handle;

    public LivingEntity(net.minecraft.world.entity.LivingEntity handle) {
        super(handle);
        this.handle = handle;
    }

    /** 内部助手：强转为原生活体（构造器已传入非空活体）。 */
    private net.minecraft.world.entity.LivingEntity living() {
        return handle;
    }

    // ---------- 生命 / 呼吸 / 吸收 ----------

    public float getHealth() {
        return handle == null ? 0.0F : handle.getHealth();
    }

    public float getMaxHealth() {
        return handle == null ? 0.0F : handle.getMaxHealth();
    }

    /** 直接设置当前生命值（不触发治疗/伤害逻辑）。 */
    public void setHealth(float health) {
        if (handle != null) {
            handle.setHealth(health);
        }
    }

    public boolean isDeadOrDying() {
        return handle != null && handle.isDeadOrDying();
    }

    /** 剩余空气值（水下计时）。 */
    public int getAirSupply() {
        return handle == null ? 0 : handle.getAirSupply();
    }

    public void setAirSupply(int air) {
        if (handle != null) {
            handle.setAirSupply(air);
        }
    }

    /** 额外生命值（吸收心）。 */
    public float getAbsorptionAmount() {
        return handle == null ? 0.0F : handle.getAbsorptionAmount();
    }

    public void setAbsorptionAmount(float amount) {
        if (handle != null) {
            handle.setAbsorptionAmount(amount);
        }
    }

    // ---------- 手持物品（框架包装版，对齐 Player） ----------

    public ItemStack getMainHandItem() {
        return new ItemStack(handle.getMainHandItem());
    }

    public ItemStack getOffhandItem() {
        return new ItemStack(handle.getOffhandItem());
    }

    // ---------- 治疗 / 伤害 ----------

    /** 治疗指定生命值（按生物法则，忽略对不死生物的负面效果差异）。 */
    public void heal(float amount) {
        if (handle != null) {
            handle.heal(amount);
        }
    }

    /**
     * 以通用伤害源对生物造成伤害。
     *
     * @param amount 伤害量（按 {@code float} 语义）
     * @return 是否确实造成了伤害（护甲/魔免可能拦截）
     */
    public boolean damage(double amount) {
        if (handle == null) {
            return false;
        }
        return handle.hurt(handle.level().damageSources().generic(), (float) amount);
    }

    // ---------- 朝向 / 视线 ----------

    /** 当前视线方向向量（原生存生）。 */
    public Vec3 getLookAngle() {
        return handle == null ? Vec3.ZERO : handle.getLookAngle();
    }

    /** 是否对火焰免疫。 */
    public boolean isImmuneToFire() {
        return handle != null && handle.fireImmune();
    }

    /** 是否完全浸没在水中。 */
    public boolean isUnderWater() {
        return handle != null && handle.isUnderWater();
    }

    /** 眼睛高度（相对实体脚底）。 */
    public float getEyeHeight() {
        return handle == null ? 0.0F : handle.getEyeHeight();
    }

    /** 眼睛位置（世界坐标）。 */
    public Vec3 getEyePosition() {
        return handle == null ? Vec3.ZERO : handle.getEyePosition();
    }

    /** 本生物与目标之间是否有无障碍直线视线。 */
    public boolean hasLineOfSight(Entity target) {
        return handle != null && target != null && target.getHandle() != null
                && handle.hasLineOfSight(target.getHandle());
    }

    /** 最近伤到本生物的活体；无则为 null。 */
    public LivingEntity getLastHurtByMob() {
        net.minecraft.world.entity.LivingEntity m = handle == null ? null : handle.getLastHurtByMob();
        return m == null ? null : new LivingEntity(m);
    }

    /**
     * 最近一次伤害的<b>来源类型</b>（原生逃生口）。
     * <p>
     * 与 {@link #getLastHurtByMob()} 分工互补：本方法能取到<em>非活体实体直接攻击</em>造成的伤害，
     * 如苦力怕爆炸（{@code creeper}）、摔落（{@code fall}）、末影珍珠等环境/间接伤害，
     * 而这些 {@code getLastHurtByMob()} 拿不到。未受伤（尚未受到伤害）返回 {@code null}；
     * 实体死亡重生后被替换，来源清空亦返回 {@code null}（属预期）。
     *
     * @return 原生 {@link net.minecraft.world.damagesource.DamageSource}，未受伤为 {@code null}
     */
    @org.jetbrains.annotations.Nullable
    public net.minecraft.world.damagesource.DamageSource getLastDamageSource() {
        return handle == null ? null : handle.getLastDamageSource();
    }

    /**
     * 最近一次伤害的可读<b>类型标识</b>字符串，供聊天栏直接打印。
     * <p>
     * 优先返回伤害类型注册键的 ResourceLocation 字符串（形如 {@code "minecraft:creeper"}、
     * {@code "minecraft:fall"}），取不到时回退到原生的消息 id（{@code DamageType.msgId()}）。
     * 与 {@link #getLastHurtByMob()} 分工参见 {@link #getLastDamageSource()}。
     *
     * @return 可读伤害类型标识；未受伤（或来源类型无注册键且无消息 id）为 {@code null}
     */
    @org.jetbrains.annotations.Nullable
    public String getLastDamageCause() {
        net.minecraft.world.damagesource.DamageSource s = getLastDamageSource();
        if (s == null) {
            return null;
        }
        var key = s.typeHolder().unwrapKey();
        if (key.isPresent()) {
            return key.get().location().toString();
        }
        return s.type().msgId();
    }

    @Override
    public String toString() {
        return "LivingEntity{" + getType() + " hp=" + getHealth() + "/" + getMaxHealth() + "}";
    }
}