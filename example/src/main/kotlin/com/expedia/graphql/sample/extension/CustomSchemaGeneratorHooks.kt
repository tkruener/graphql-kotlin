package com.expedia.graphql.sample.extension

import com.expedia.graphql.directives.KotlinDirectiveWiringFactory
import com.expedia.graphql.execution.DataFetcherExecutionPredicate
import com.expedia.graphql.hooks.GraphQlTypeExtender
import com.expedia.graphql.hooks.SchemaGeneratorHooks
import com.expedia.graphql.hooks.TopLevelType
import com.expedia.graphql.sample.resolvers.GraphlQlCustomArgumentResolver
import com.expedia.graphql.sample.resolvers.GraphlQlTypeExtenderProvider
import com.expedia.graphql.sample.resolvers.Mutation
import com.expedia.graphql.sample.resolvers.Query
import com.expedia.graphql.sample.resolvers.Resolver
import com.expedia.graphql.sample.resolvers.Subscription
import com.expedia.graphql.sample.resolvers.isAnnotatedWith
import com.expedia.graphql.sample.validation.DataFetcherExecutionValidator
import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType
import org.springframework.stereotype.Component
import java.util.UUID
import javax.validation.Validator
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.jvmErasure

/**
 * Schema generator hook that adds additional scalar types.
 */
@Component
class CustomSchemaGeneratorHooks(validator: Validator,
								 override val wiringFactory: KotlinDirectiveWiringFactory,
								 private val schemaExtender: GraphlQlTypeExtenderProvider,
								 override val parameterResolver: GraphlQlCustomArgumentResolver) : SchemaGeneratorHooks {

	/**
	 * Register additional GraphQL scalar types.
	 */
	override fun willGenerateGraphQLType(type: KType): GraphQLType? = when (type.classifier) {
		UUID::class -> graphqlUUIDType
		else -> null
	}

	override val dataFetcherExecutionPredicate: DataFetcherExecutionPredicate? = DataFetcherExecutionValidator(validator)

	override fun getTypeExtenders(kClass: KClass<*>): List<GraphQlTypeExtender> = schemaExtender.getTypeExtendersForType(kClass)

	override fun isValidTopLevelFunction(topLevelType: TopLevelType, function: KFunction<*>) = when {
		function.isTargetingResolver() -> function.isResolverTopLevelFunction(topLevelType)
		else -> super.isValidTopLevelFunction(topLevelType, function)
	}

	private fun KFunction<*>.isResolverTopLevelFunction(topLevelType: TopLevelType) = when (topLevelType) {
		TopLevelType.Query -> isAnnotatedWith<Query>()
		TopLevelType.Mutation -> isAnnotatedWith<Mutation>()
		TopLevelType.Subscription -> isAnnotatedWith<Subscription>()
	}

	private fun KFunction<*>.isTargetingResolver() = this.instanceParameter?.let {
		it.type.jvmErasure.superclasses.contains(Resolver::class)
	} ?: false
}

internal val graphqlUUIDType = GraphQLScalarType("UUID",
		"A type representing a formatted java.util.UUID",
		UUIDCoercing
)

private object UUIDCoercing : Coercing<UUID, String> {
	override fun parseValue(input: Any?): UUID = UUID.fromString(
			serialize(
					input
			)
	)

	override fun parseLiteral(input: Any?): UUID? {
		val uuidString = (input as? StringValue)?.value
		return UUID.fromString(uuidString)
	}

	override fun serialize(dataFetcherResult: Any?): String = dataFetcherResult.toString()
}