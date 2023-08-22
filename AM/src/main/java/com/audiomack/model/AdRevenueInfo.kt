package com.audiomack.model

import org.json.JSONObject

data class AdRevenueInfo(
    val adGroupPriority: Int,
    val adUnitFormat: String,
    val country: String,
    val publisherRevenue: Double,
    val precision: String,
    val impressionId: String,
    val adGroupId: String,
    val adUnitId: String,
    val adGroupType: String,
    val currency: String,
    val adUnitName: String,
    val adGroupName: String,
    val networkName: String,
    val networkPlacementId: String
) {
    constructor(jsonObject: JSONObject) : this(
        jsonObject.optInt("adgroup_priority", 0),
        jsonObject.optString("adunit_format", ""),
        jsonObject.optString("country", ""),
        jsonObject.optDouble("publisher_revenue", 0.0),
        jsonObject.optString("precision", ""),
        jsonObject.optString("id", ""),
        jsonObject.optString("adgroup_id", ""),
        jsonObject.optString("adunit_id", ""),
        jsonObject.optString("adgroup_type", ""),
        jsonObject.optString("currency", ""),
        jsonObject.optString("adunit_name", ""),
        jsonObject.optString("adgroup_name", ""),
        jsonObject.optString("network_name", ""),
        jsonObject.optString("network_placement_id", "")
    )

    override fun toString() =
        "Ad Group Name: $adGroupName, " +
        "Ad Group Priority: $adGroupPriority, " +
        "Ad Group Type: $adGroupType, " +
        "Ad Group ID: $adGroupId, " +
        "Ad Unit Name: $adUnitName, " +
        "Ad Unit Format: $adUnitFormat, " +
        "Ad Unit ID: $adUnitId, " +
        "Publisher Revenue: $publisherRevenue, " +
        "Currency: $currency, " +
        "Precision: $precision, " +
        "Network Name: $networkName, " +
        "Network Placement ID: $networkPlacementId, " +
        "Impression ID: $impressionId, " +
        "Country: $country"
}
