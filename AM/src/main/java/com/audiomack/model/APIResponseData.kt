package com.audiomack.model

import java.util.Collections

class APIResponseData(var objects: List<Any>, val pagingToken: String?, val ignore: Boolean? = false, val related: Boolean? = false) {

    constructor() : this(Collections.emptyList(), null, false)

    constructor(objects: List<Any>, pagingToken: String?) : this(objects, pagingToken, false)
}
