package com.expedia.graphql.sample.resolvers

import com.expedia.graphql.paramters.CustomGraphQlParameterResolver
import graphql.schema.DataFetchingEnvironment
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.jvm.jvmErasure

@Component
class GraphlQlCustomArgumentResolver : CustomGraphQlParameterResolver {

	//The result of this function should be cached for performance reasons
	override fun isCustomlyResolvedArgument(function: KFunction<*>, kParameter: KParameter): Boolean {
		val targetType = function.instanceParameter?.type?.jvmErasure
		val paramterType = kParameter.type.jvmErasure
		val resolverType = targetType?.ifSubClass<Resolver<*>, KClass<*>> {
			it.getGenericTypeClass()
		}

		fun isSelfInvocation() = !function.isAnnotatedWith<Mutation>() and
				((paramterType == targetType) or (paramterType == resolverType))


		return isSelfInvocation() || kParameter.isAnnotatedWith<Parent>()
	}

	override fun resolve(instance: Any, parameter: KParameter, dataFetchingEnvironment: DataFetchingEnvironment) =
			dataFetchingEnvironment.getSource<Any>()

}