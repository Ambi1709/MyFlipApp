#
# Copyright (C) 2015 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
ifneq ($(TARGET_BUILD_PDK), true)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src) $(call all-Iaidl-files-under, src)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_PACKAGE_NAME := CarMediaPSA

LOCAL_OVERRIDES_PACKAGES := CarMediaApp

LOCAL_CERTIFICATE := platform

LOCAL_MODULE_TAGS := optional

LOCAL_PRIVILEGED_MODULE := true

LOCAL_STATIC_JAVA_LIBRARIES += android-support-v4
LOCAL_STATIC_ANDROID_LIBRARIES := android-support-v4

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res 
		
# Include support-v7-appcompat, if not already included
ifeq (,$(findstring android-support-v7-appcompat,$(LOCAL_STATIC_JAVA_LIBRARIES)))
LOCAL_RESOURCE_DIR += frameworks/support/v7/appcompat/res
LOCAL_RESOURCE_DIR += frameworks/support/design/res

LOCAL_AAPT_FLAGS += --extra-packages android.support.v7.appcompat
LOCAL_AAPT_FLAGS += --extra-packages android.support.design
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v7-appcompat
LOCAL_STATIC_JAVA_LIBRARIES += android-support-design
endif

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_DEX_PREOPT := false

include packages/apps/Car/libs/car-stream-ui-lib/car-stream-ui-lib.mk
include packages/apps/Car/libs/car-apps-common/car-apps-common.mk
include packages/services/Car/car-support-lib/car-support.mk
include vendor/harman/packages/apps/libs/car-widget-psa/widget/src/main/car-psa-widget-lib.mk
include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
include $(BUILD_MULTI_PREBUILT)
endif

