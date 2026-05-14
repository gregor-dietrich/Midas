# Coverage Report

## Summary by Module

| Module    |    Line % |  Branch % |
| :-------- | --------: | --------: |
| API       |     92.7% |     76.6% |
| GUI       |     91.5% |     77.8% |
| **Total** | **92.3%** | **76.9%** |

## By Package (sorted by line coverage)

| Package                     | Lines Missed | Lines Covered |    Line % |  Branch % |
| :-------------------------- | -----------: | ------------: | --------: | --------: |
| de.vptr.midas.api.util      |            9 |            42 |     82.4% |     72.5% |
| de.vptr.midas.api.security  |           36 |           269 |     88.2% |     78.2% |
| de.vptr.midas.api.exception |            4 |            40 |     90.9% |     65.6% |
| de.vptr.midas.gui.service   |           80 |           846 |     91.4% |     77.4% |
| de.vptr.midas.api.service   |           64 |           941 |     93.6% |     76.7% |
| de.vptr.midas.api.resource  |           20 |           322 |     94.2% |     72.2% |
| de.vptr.midas.api.entity    |            6 |           108 |     94.7% |     86.7% |
| de.vptr.midas.gui.util      |            2 |            40 |     95.2% |     80.0% |
| de.vptr.midas.api.filter    |            4 |           107 |     96.4% |     81.0% |
| **Total**                   |      **225** |      **2715** | **92.3%** | **76.9%** |

## By File (sorted by line coverage)

> **Note:** Files with full line *and* branch coverage have been omitted.

| Package                     | Class                       | Lines Missed | Lines Covered | Line % | Branch % |
| :-------------------------- | :-------------------------- | -----------: | ------------: | -----: | -------: |
| de.vptr.midas.api.util      | UlidUtil                    |            4 |             1 |  20.0% |     0.0% |
| de.vptr.midas.api.entity    | UserGroupMetaEntity         |            3 |             2 |  40.0% |    50.0% |
| de.vptr.midas.api.util      | QueryAggregationUtil        |            5 |            10 |  66.7% |    66.7% |
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
| de.vptr.midas.api.exception | GenericExceptionMapper      |            4 |            29 |  87.9% |    65.4% |
| de.vptr.midas.api.security  | PasswordHashingService      |            1 |             8 |  88.9% |    90.0% |
| de.vptr.midas.gui.service   | PostCategoryService         |            5 |            41 |  89.1% |    40.0% |
| de.vptr.midas.api.resource  | PostResource                |            4 |            34 |  89.5% |    70.0% |
| de.vptr.midas.api.service   | UserGroupService            |            8 |            69 |  89.6% |    64.7% |
| de.vptr.midas.api.service   | PostCategoryService         |            8 |            77 |  90.6% |    75.0% |
| de.vptr.midas.gui.service   | AccountService              |           12 |           117 |  90.7% |    61.5% |
| de.vptr.midas.api.entity    | UserGroupEntity             |            1 |            10 |  90.9% |    87.5% |
| de.vptr.midas.api.service   | AccountResponseAssembler    |            1 |            10 |  90.9% |    50.0% |
| de.vptr.midas.api.service   | UserRankService             |           10 |           103 |  91.2% |    81.4% |
| de.vptr.midas.api.resource  | UserResource                |            4 |            42 |  91.3% |    77.8% |
| de.vptr.midas.gui.service   | PostService                 |            4 |            46 |  92.0% |    75.0% |
| de.vptr.midas.api.service   | PostCommentService          |            6 |            70 |  92.1% |    71.1% |
| de.vptr.midas.api.service   | AccountService              |            3 |            37 |  92.5% |    66.7% |
| de.vptr.midas.api.resource  | UserGroupResource           |            2 |            27 |  93.1% |    75.0% |
| de.vptr.midas.api.service   | AccountAccessService        |           10 |           140 |  93.3% |    67.1% |
| de.vptr.midas.gui.util      | ErrorMessageUtil            |            2 |            31 |  93.9% |    75.0% |
| de.vptr.midas.api.resource  | UserRankResource            |            1 |            16 |  94.1% |    66.7% |
| de.vptr.midas.api.resource  | PostCommentResource         |            2 |            33 |  94.3% |    64.3% |
| de.vptr.midas.api.resource  | PageResource                |            1 |            17 |  94.4% |    66.7% |
| de.vptr.midas.api.security  | UserRankIdentityAugmentor   |            2 |            35 |  94.6% |    75.0% |
| de.vptr.midas.api.service   | UserService                 |            6 |           109 |  94.8% |    75.8% |
| de.vptr.midas.api.service   | PaymentService              |            8 |           155 |  95.1% |    83.1% |
| de.vptr.midas.api.resource  | AccountResource             |            5 |            99 |  95.2% |    75.0% |
| de.vptr.midas.gui.service   | SessionAttribute            |            1 |            20 |  95.2% |    70.0% |
| de.vptr.midas.api.filter    | RateLimitFilter             |            4 |            89 |  95.7% |    80.8% |
| de.vptr.midas.api.security  | AuthFailureRateLimitService |            1 |            25 |  96.2% |    75.0% |
| de.vptr.midas.api.service   | PostService                 |            4 |           119 |  96.7% |    82.8% |
| de.vptr.midas.api.resource  | PaymentResource             |            1 |            30 |  96.8% |    66.7% |
| de.vptr.midas.gui.service   | PostCommentService          |            2 |            67 |  97.1% |    75.0% |
| de.vptr.midas.gui.service   | UserGroupService            |            1 |            48 |  98.0% |    75.0% |
| de.vptr.midas.api.exception | ValidationExceptionMapper   |            0 |             4 | 100.0% |    66.7% |
| de.vptr.midas.api.entity    | BaseEntity                  |            0 |             6 | 100.0% |    87.5% |
| de.vptr.midas.api.filter    | RateLimitFilter.BucketStore |            0 |            15 | 100.0% |    83.3% |
| de.vptr.midas.api.security  | AuthFailureRateLimitFilter  |            0 |            27 | 100.0% |    75.0% |
| de.vptr.midas.api.service   | AdminAccessService          |            0 |            10 | 100.0% |    83.3% |
| de.vptr.midas.api.service   | PageService                 |            0 |            29 | 100.0% |    85.7% |
| de.vptr.midas.api.util      | IpUtil                      |            0 |            11 | 100.0% |    83.3% |
| de.vptr.midas.gui.service   | UserRankService             |            0 |            88 | 100.0% |    90.0% |
| de.vptr.midas.gui.service   | ThemeService                |            0 |            31 | 100.0% |    80.0% |
