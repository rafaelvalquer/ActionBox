package com.luminor.actionbox.domain.search

import java.text.Normalizer
import java.util.Locale

object SearchNormalizer {
    fun normalize(value: String): String = Normalizer
        .normalize(value.trim(), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase(Locale.ROOT)
        .replace("\\s+".toRegex(), " ")
}
