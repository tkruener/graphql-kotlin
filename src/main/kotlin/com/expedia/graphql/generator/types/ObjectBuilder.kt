package com.expedia.graphql.generator.types

import com.expedia.graphql.generator.SchemaGenerator
import com.expedia.graphql.generator.TypeBuilder
import com.expedia.graphql.generator.extensions.getGraphQLDescription
import com.expedia.graphql.generator.extensions.getSimpleName
import com.expedia.graphql.generator.extensions.getValidFunctions
import com.expedia.graphql.generator.extensions.getValidProperties
import com.expedia.graphql.generator.extensions.getValidSuperclasses
import com.expedia.graphql.generator.extensions.safeCast
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLType
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

internal class ObjectBuilder(generator: SchemaGenerator) : TypeBuilder(generator) {

    internal fun objectType(kClass: KClass<*>, interfaceType: GraphQLInterfaceType? = null): GraphQLType {

        return state.cache.buildIfNotUnderConstruction(kClass) {
            val builder = GraphQLObjectType.newObject()

            val name = kClass.getSimpleName()
            builder.name(name)
            builder.description(kClass.getGraphQLDescription())

            generator.directives(kClass).forEach {
                builder.withDirective(it)
            }

            if (interfaceType != null) {
                builder.withInterface(interfaceType)
            } else {
                kClass.getValidSuperclasses(config.hooks)
                    .map { objectFromReflection(it.createType(), false) }
                    .forEach {
                        when (it) {
                            is GraphQLInterfaceType -> builder.withInterface(it)
                        }
                    }
            }

            kClass.getValidProperties(config.hooks)
                .forEach { builder.field(generator.property(it, kClass)) }

            kClass.getValidFunctions(config.hooks)
                .forEach { builder.field(generator.function(it, name)) }

            builder.addExtenders(it)

            config.hooks.onRewireGraphQLType(builder.build()).safeCast()
        }
    }

    private fun GraphQLObjectType.Builder.addExtenders(kClass: KClass<*>) {
        val extenders = config.hooks.getTypeExtenders(kClass)
        extenders.forEach { extender ->
            extender.functions.forEach {
                field(generator.function(it, kClass.getSimpleName(), extender.target))
            }
        }
    }
}
