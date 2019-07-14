package com.expedia.graphql.sample

import com.expedia.graphql.SchemaGeneratorConfig
import com.expedia.graphql.TopLevelObject
import com.expedia.graphql.execution.KotlinDataFetcherFactoryProvider
import com.expedia.graphql.extensions.print
import com.expedia.graphql.hooks.SchemaGeneratorHooks
import com.expedia.graphql.sample.datafetchers.CustomDataFetcherFactoryProvider
import com.expedia.graphql.sample.datafetchers.SpringDataFetcherFactory
import com.expedia.graphql.sample.directives.CustomDirectiveWiringFactory
import com.expedia.graphql.sample.exceptions.CustomDataFetcherExceptionHandler
import com.expedia.graphql.sample.mutation.Mutation
import com.expedia.graphql.sample.query.Query
import com.expedia.graphql.sample.resolvers.Resolver
import com.expedia.graphql.sample.subscriptions.Subscription
import com.expedia.graphql.toSchema
import graphql.GraphQL
import graphql.execution.AsyncExecutionStrategy
import graphql.execution.AsyncSerialExecutionStrategy
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.SubscriptionExecutionStrategy
import graphql.schema.GraphQLSchema
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter

@SpringBootApplication
class Application {

	private val logger = LoggerFactory.getLogger(Application::class.java)

	@Bean
	fun wiringFactory() = CustomDirectiveWiringFactory()

	@Bean
	fun dataFetcherFactoryProvider(springDataFetcherFactory: SpringDataFetcherFactory, hooks: SchemaGeneratorHooks) =
			CustomDataFetcherFactoryProvider(springDataFetcherFactory, hooks)

	@Bean
	fun schemaConfig(hooks: SchemaGeneratorHooks, dataFetcherFactoryProvider: KotlinDataFetcherFactoryProvider): SchemaGeneratorConfig = SchemaGeneratorConfig(
			supportedPackages = listOf("com.expedia"),
			hooks = hooks,
			dataFetcherFactoryProvider = dataFetcherFactoryProvider
	)

	@Bean
	fun schema(
			queries: List<Query>,
			mutations: List<Mutation>,
			subscriptions: List<Subscription>,
			resolvers: List<Resolver<*>>,
			schemaConfig: SchemaGeneratorConfig
	): GraphQLSchema {
		fun List<Any>.toTopLevelObjects() = (this + resolvers).map {
			TopLevelObject(it)
		}

		val schema = toSchema(
				config = schemaConfig,
				queries = queries.toTopLevelObjects(),
				mutations = mutations.toTopLevelObjects(),
				subscriptions = subscriptions.toTopLevelObjects()
		)

		logger.info(schema.print())

		return schema
	}

	@Bean
	fun dataFetcherExceptionHandler(): DataFetcherExceptionHandler = CustomDataFetcherExceptionHandler()

	@Bean
	fun subscriptionHandler(graphQL: GraphQL) = SubscriptionHandler(graphQL)

	@Bean
	fun websocketHandlerAdapter() = WebSocketHandlerAdapter()

	@Bean
	fun graphQL(
			schema: GraphQLSchema,
			dataFetcherExceptionHandler: DataFetcherExceptionHandler
	): GraphQL = GraphQL.newGraphQL(schema)
			.queryExecutionStrategy(AsyncExecutionStrategy(dataFetcherExceptionHandler))
			.mutationExecutionStrategy(AsyncSerialExecutionStrategy(dataFetcherExceptionHandler))
			.subscriptionExecutionStrategy(SubscriptionExecutionStrategy(dataFetcherExceptionHandler))
			.build()
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
