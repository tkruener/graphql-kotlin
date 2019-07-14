package com.expedia.graphql.paramters

import graphql.schema.DataFetchingEnvironment
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

/**
 * Interface for customized resolution of graphql field arguments. This allows user of this library.
 */
interface CustomGraphQlParameterResolver {

    /**
     * Returns whether the given argument will be resolved by this resolver. If true, the schema generator
     * will ignore this field schema generation and call the resolve function at runtime to provide an instance
     * of this field.
     */
    fun isCustomlyResolvedArgument(function: KFunction<*>, kParameter: KParameter) = false

    /**
     * Resolves the given argument at runtime. This function is only executed if the argument should
     * be customly resolved.
     */
    fun resolve(instance: Any, parameter: KParameter, dataFetchingEnvironment: DataFetchingEnvironment): Any =
        error("Resolve function not implemented even though argument ${parameter.name} should be resolved customly.")
}

/**
 * Adds no customly resolved parameters to the schema.
 */
object NoCustomParamters : CustomGraphQlParameterResolver
