package com.expedia.graphql.sample.resolvers

import com.expedia.graphql.annotations.GraphQLIgnore
import com.expedia.graphql.hooks.GraphQlTypeExtender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.jvmErasure


open class Resolver<X> : CoroutineScope {
	override val coroutineContext = Dispatchers.IO
}

@Component
class GraphlQlTypeExtenderProvider {

	@Autowired
	private lateinit var resolvers: List<Resolver<*>>

	private val javaObjectFunctionNames = Any::class.memberFunctions.map { it.name }

	private val topLevelAnnotations = listOf(Query::class, Mutation::class, Subscription::class)

	private val ktypeToResolver by lazy {
		resolvers.groupBy(
				{ it.getGenericTypeClass() },
				{ resolver ->
					resolver::class.memberFunctions
							.filter { it.visibility == KVisibility.PUBLIC }
							.filter { it.annotations.none { it is GraphQLIgnore } }
							.filterNot { it.name in javaObjectFunctionNames }
							.filterNot { it.isRootFunction() }
							.filterNot { it.isParentResolver() }
							.let {
								GraphQlTypeExtender(resolver, it)
							}
				}
		)
	}

	private fun KFunction<*>.isRootFunction() = annotations.any { annotation ->
		val superTypes = annotation::class.supertypes.map { it.jvmErasure }
		return superTypes.intersect(topLevelAnnotations).isNotEmpty()
	}

	private val ktypeToChildResolvers by lazy {
		val result = mutableMapOf<KClass<*>, MutableList<GraphQlTypeExtender>>()
		resolvers.map {
			val kClass = it::class
			kClass.memberFunctions.filter { it.isParentResolver() }.forEach { function ->
				function.parameters.find { param ->
					param.annotations.any { it is Parent }
				}?.let { param ->
					result.compute(param.type.classifier as KClass<*>) { _, list ->
						val extender = GraphQlTypeExtender(it, listOf(function))
						list?.also { it.add(extender) } ?: mutableListOf(extender)
					}
				}
			}
		}
		result
	}

	private fun KFunction<*>.isParentResolver() = parameters.any {
		it.isAnnotatedWith<Parent>()
	}

	fun getTypeExtendersForType(kClass: KClass<*>) =
			ktypeToChildResolvers.getOrDefault(kClass, mutableListOf()) + ktypeToResolver.getOrDefault(kClass, mutableListOf())

}


