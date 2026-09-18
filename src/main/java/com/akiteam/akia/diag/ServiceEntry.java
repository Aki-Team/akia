package com.akiteam.akia.diag;

/**
 * 一条已注册的服务提供者记录（诊断数据的扁平化表示）。
 *
 * @param serviceClass 服务接口全限定名
 * @param providerClass 服务实现类全限定名
 * @param plugin     注册该服务的插件名
 * @param priority   注册时声明的优先级
 */
public record ServiceEntry(String serviceClass, String providerClass, String plugin, String priority) {
}