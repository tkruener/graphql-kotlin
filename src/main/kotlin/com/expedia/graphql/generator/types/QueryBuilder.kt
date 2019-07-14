package com.expedia.graphql.generator.types

import com.expedia.graphql.TopLevelObject
import com.expedia.graphql.exceptions.InvalidQueryTypeException
import com.expedia.graphql.exceptions.InvalidSchemaException
import com.expedia.graphql.generator.SchemaGenerator
import com.expedia.graphql.generator.TypeBuilder
import com.expedia.graphql.generator.extensions.getValidTopLevelFunctions
import com.expedia.graphql.generator.extensions.isNotPublic
import com.expedia.graphql.hooks.TopLevelType
import graphql.schema.GraphQLObjectType

internal class QueryBuilder(generator: SchemaGenerator) : TypeBuilder(generator) {

    @Throws(InvalidSchemaException::class)
    fun getQueryObject(queries: List<TopLevelObject>): GraphQLObjectType {

        if (queries.isEmpty()) {
            throw InvalidSchemaException()
        }

        val queryBuilder = GraphQLObjectType.Builder()
        queryBuilder.name(config.topLevelNames.query)

        for (query in queries) {
            if (query.kClass.isNotPublic()) {
                throw InvalidQueryTypeException(query.kClass)
            }

            generator.directives(query.kClass).forEach {
                queryBuilder.withDirective(it)
            }

            query.kClass.getValidTopLevelFunctions(config.hooks, TopLevelType.Query)
                .forEach {
                    // NEED BUILT QUERY
                    val function = generator.function(it, config.topLevelNames.query, query.obj)
                    val functionFromHook = config.hooks.didGenerateQueryType(it, function)
                    queryBuilder.field(functionFromHook)
                }
        }

        return queryBuilder.build()
    }
}
