/*
 * Copyright (C) 2021 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include <libinit_dalvik_heap.h>
#include <libinit_variant.h>

#include "vendor_init.h"

static const variant_info_t aliothin_info = {
    .hwc_value = "INDIA",
    .sku_value = "",

    .brand = "Mi",
    .device = "aliothin",
    .name = "aliothin",
    .flavor = "aliothin-user",
    .marketname = "Mi 11X",
    .model = "M2012K11AI",

    .build_fingerprint = "Mi/aliothin/aliothin:13/TKQ1.221114.001/V816.0.1.0.TKHINXM:user/release-keys",
    .build_description = "aliothin-user 13 TKQ1.221114.001 V816.0.1.0.TKHINXM release-keys",

    .nfc = false,
};

static const variant_info_t alioth_global_info = {
    .hwc_value = "GLOBAL",
    .sku_value = "",

    .brand = "POCO",
    .device = "alioth",
    .name = "alioth_global",
    .flavor = "alioth_global-user",
    .marketname = "POCO F3",
    .model = "M2012K11AG",

    .build_fingerprint = "POCO/alioth_global/alioth:13/TKQ1.221114.001/V816.0.2.0.TKHMIXM:user/release-keys",
    .build_description = "alioth_global-user 13 TKQ1.221114.001 V816.0.2.0.TKHMIXM release-keys",

    .nfc = true,
};

static const variant_info_t alioth_info = {
    .hwc_value = "",
    .sku_value = "",

    .brand = "Redmi",
    .device = "alioth",
    .name = "alioth",
    .flavor = "alioth-user",
    .marketname = "Redmi K40",
    .model = "M2012K11AC",

    .build_fingerprint = "Redmi/alioth/alioth:13/TKQ1.221114.001/V816.0.3.0.TKHCNXM:user/release-keys",
    .build_description = "alioth-user 13 TKQ1.221114.001 V816.0.3.0.TKHCNXM release-keys",

    .nfc = true,
};

static const std::vector<variant_info_t> variants = {
    aliothin_info,
    alioth_global_info,
    alioth_info,
};

void vendor_load_properties() {
    search_variant(variants);
    set_dalvik_heap();
}
