# Coverage Report

## Summary by Module

| Module    |    Line % |  Branch % |
| :-------- | --------: | --------: |
| API       |     94.1% |     81.7% |
| GUI       |     93.3% |     82.9% |
| **Total** | **93.8%** | **81.9%** |

## By Package (sorted by line coverage)

| Package                     | Lines Missed | Lines Covered |    Line % |  Branch % |
| :-------------------------- | -----------: | ------------: | --------: | --------: |
| de.vptr.midas.api.security  |           34 |           273 |     88.9% |     80.9% |
| de.vptr.midas.gui.service   |           63 |           863 |     93.2% |     83.4% |
| de.vptr.midas.api.service   |           57 |           951 |     94.3% |     79.7% |
| de.vptr.midas.api.entity    |            6 |           108 |     94.7% |     90.0% |
| de.vptr.midas.gui.util      |            2 |            40 |     95.2% |     80.0% |
| de.vptr.midas.api.resource  |           16 |           326 |     95.3% |     82.2% |
| de.vptr.midas.api.filter    |            4 |           107 |     96.4% |     81.0% |
| de.vptr.midas.api.exception |            0 |            44 |    100.0% |     96.9% |
| de.vptr.midas.api.util      |            0 |            51 |    100.0% |     92.5% |
| **Total**                   |      **182** |      **2763** | **93.8%** | **81.9%** |

## By File (sorted by line coverage)

> **Note:** Files with full line _and_ branch coverage have been omitted.

| Package                     | Class                       | Lines Missed | Lines Covered | Line % | Branch % |
| :-------------------------- | :-------------------------- | -----------: | ------------: | -----: | -------: |
| de.vptr.midas.api.entity    | UserGroupMetaEntity         |            3 |             2 |  40.0% |   100.0% |
| de.vptr.midas.gui.service   | SessionKeyManager           |           12 |            34 |  73.9% |    70.0% |
| de.vptr.midas.api.security  | EncryptionKeyManager        |           12 |            37 |  75.5% |    75.0% |
| de.vptr.midas.gui.service   | BackendAvailabilityService  |            5 |            17 |  77.3% |   100.0% |
| de.vptr.midas.gui.service   | SessionAuthorizationStore   |           12 |            44 |  78.6% |    83.3% |
| de.vptr.midas.gui.service   | BackendHealthCheck          |            3 |            12 |  80.0% |    75.0% |
| de.vptr.midas.api.security  | UserIdentityProvider        |           12 |            49 |  80.3% |    65.6% |
| de.vptr.midas.api.entity    | PostCategoryEntity          |            1 |             5 |  83.3% |    75.0% |
| de.vptr.midas.gui.service   | UserService                 |            7 |            39 |  84.8% |   100.0% |
| de.vptr.midas.api.entity    | UserAccountMetaEntity       |            1 |             6 |  85.7% |   100.0% |
| de.vptr.midas.api.security  | EncryptionService           |            8 |            50 |  86.2% |    88.9% |
| de.vptr.midas.gui.service   | AuthService                 |           16 |           102 |  86.4% |    72.7% |
| de.vptr.midas.api.security  | PasswordHashingService      |            1 |             8 |  88.9% |    90.0% |
| de.vptr.midas.api.entity    | UserGroupEntity             |            1 |            10 |  90.9% |    87.5% |
| de.vptr.midas.api.service   | UserRankService             |           10 |           103 |  91.2% |    88.6% |
| de.vptr.midas.api.resource  | AccountResource             |            9 |            95 |  91.3% |    70.0% |
| de.vptr.midas.api.service   | PostCategoryService         |            7 |            78 |  91.8% |    78.1% |
| de.vptr.midas.gui.service   | PostService                 |            4 |            46 |  92.0% |    75.0% |
| de.vptr.midas.api.resource  | PostResource                |            3 |            35 |  92.1% |    90.0% |
| de.vptr.midas.api.service   | PostCommentService          |            6 |            70 |  92.1% |    71.1% |
| de.vptr.midas.api.service   | AccountAccessService        |           11 |           139 |  92.7% |    65.9% |
| de.vptr.midas.api.resource  | UserResource                |            3 |            43 |  93.5% |    77.8% |
| de.vptr.midas.api.service   | UserGroupService            |            5 |            72 |  93.5% |    82.4% |
| de.vptr.midas.gui.util      | ErrorMessageUtil            |            2 |            31 |  93.9% |    75.0% |
| de.vptr.midas.api.service   | UserService                 |            6 |           109 |  94.8% |    77.3% |
| de.vptr.midas.api.service   | PaymentService              |            8 |           158 |  95.2% |    83.3% |
| de.vptr.midas.gui.service   | SessionAttribute            |            1 |            20 |  95.2% |    70.0% |
| de.vptr.midas.api.filter    | RateLimitFilter             |            4 |            89 |  95.7% |    80.8% |
| de.vptr.midas.api.resource  | UserGroupResource           |            1 |            28 |  96.6% |    87.5% |
| de.vptr.midas.api.service   | PostService                 |            4 |           119 |  96.7% |    82.8% |
| de.vptr.midas.gui.service   | PostCommentService          |            2 |            67 |  97.1% |    75.0% |
| de.vptr.midas.api.security  | UserRankIdentityAugmentor   |            1 |            36 |  97.3% |    87.5% |
| de.vptr.midas.gui.service   | UserGroupService            |            1 |            48 |  98.0% |    75.0% |
| de.vptr.midas.api.resource  | PageResource                |            0 |            18 | 100.0% |    83.3% |
| de.vptr.midas.api.resource  | PostCommentResource         |            0 |            35 | 100.0% |    85.7% |
| de.vptr.midas.api.resource  | UserRankResource            |            0 |            17 | 100.0% |    83.3% |
| de.vptr.midas.api.exception | GenericExceptionMapper      |            0 |            33 | 100.0% |    96.2% |
| de.vptr.midas.api.entity    | BaseEntity                  |            0 |             6 | 100.0% |    87.5% |
| de.vptr.midas.api.filter    | RateLimitFilter.BucketStore |            0 |            15 | 100.0% |    83.3% |
| de.vptr.midas.api.security  | AuthFailureRateLimitFilter  |            0 |            27 | 100.0% |    75.0% |
| de.vptr.midas.api.service   | AccountService              |            0 |            40 | 100.0% |    91.7% |
| de.vptr.midas.api.service   | AdminAccessService          |            0 |            10 | 100.0% |    83.3% |
| de.vptr.midas.api.service   | PageService                 |            0 |            29 | 100.0% |    85.7% |
| de.vptr.midas.api.util      | IpUtil                      |            0 |            11 | 100.0% |    83.3% |
| de.vptr.midas.gui.service   | UserRankService             |            0 |            88 | 100.0% |    90.0% |
| de.vptr.midas.gui.service   | AccountService              |            0 |           129 | 100.0% |    96.2% |
| de.vptr.midas.gui.service   | PostCategoryService         |            0 |            46 | 100.0% |    80.0% |
| de.vptr.midas.gui.service   | ThemeService                |            0 |            31 | 100.0% |    80.0% |
