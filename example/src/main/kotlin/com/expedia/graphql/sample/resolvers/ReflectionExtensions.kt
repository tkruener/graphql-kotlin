package com.expedia.graphql.sample.resolvers

import java.lang.reflect.ParameterizedType
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.isSubclassOf

inline fun <reified X> KFunction<*>.isAnnotatedWith() = annotations.any { it is X }

inline fun <reified X> KParameter.isAnnotatedWith() = annotations.any { it is X }

fun Resolver<*>.getGenericTypeClass(): KClass<*> = javaClass.getGenericTypeClass()

fun Class<Resolver<*>>.getGenericTypeClass(): KClass<*> = ((genericSuperclass as ParameterizedType).let {
	val typeArgument = it.actualTypeArguments.first()
	when (typeArgument) {
		is Class<*> -> typeArgument
		is ParameterizedType -> typeArgument.rawType
		else -> error("Unrecognized resolver type argument $typeArgument")
	}
}.let { Reflection.createKotlinClass(it as Class<*>) })

fun KClass<Resolver<*>>.getGenericTypeClass(): KClass<*> = this.java.getGenericTypeClass()

@Suppress("UNCHECKED_CAST")
inline fun <reified X : Any, R> KClass<*>.ifSubClass(block: (KClass<X>) -> R): R? = if (isSubclassOf(X::class)) {
	block(this as KClass<X>)
} else null