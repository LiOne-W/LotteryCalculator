# MXNZP 通用彩票信息API文档

## 概述
MXNZP提供了一套完整的彩票信息API接口，包含5个子接口，支持获取7大彩种的中奖信息、历史数据和彩种详情，同时还提供中奖结果计算功能。所有接口实时同步官方开奖信息，保证数据的准确性和及时性。

## 接口列表

### 1. 指定期号通用中奖号码

**接口说明**：获取指定期号的通用获奖号码信息

**接口地址**：`https://www.mxnzp.com/api/lottery/common/aim_lottery`

**请求方式**：GET

**请求参数**：
| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|----------|------|
| app_id | string | 是 | 应用ID（需在官网申请） |
| app_secret | string | 是 | 应用密钥（需在官网申请） |
| code | string | 是 | 彩票种类标识 |
| expect | string | 是 | 彩票期号 |

**彩票种类标识(code)**：
- ssq：双色球
- dlt：大乐透
- qlc：七乐彩
- fc3d：福彩3D
- qxc：七星彩
- pl3：排列3
- pl5：排列5
- kl8：快乐8

**请求示例**：
```
https://www.mxnzp.com/api/lottery/common/aim_lottery?expect=18135&code=ssq&app_id=your_app_id&app_secret=your_app_secret
```

**返回示例**：
```json
{
    "openCode": "01,03,06,10,11,29+16",
    "code": "ssq",
    "expect": "18135",
    "name": "双色球",
    "time": "2018-11-18 21:18:20"
}
```

**返回字段说明**：
- `openCode`：本期中奖号码（格式：前区号码+后区号码）
- `code`：彩票编号标识
- `expect`：彩票期号
- `name`：彩票名称
- `time`：发布时间

---

### 2. 最新通用中奖号码信息

**接口说明**：获取最新通用中奖号码信息

**接口地址**：`https://www.mxnzp.com/api/lottery/common/latest`

**请求方式**：GET

**请求参数**：
| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|----------|------|
| app_id | string | 是 | 应用ID |
| app_secret | string | 是 | 应用密钥 |
| code | string | 是 | 彩票种类标识 |

**请求示例**：
```
https://www.mxnzp.com/api/lottery/common/latest?code=ssq&app_id=your_app_id&app_secret=your_app_secret
```

**返回示例**：
```json
{
    "openCode": "10,12,15,25,26,27+14",
    "code": "ssq",
    "expect": "18136",
    "name": "双色球",
    "time": "2018-11-20 21:18:20"
}
```

---

### 3. 最近历史开奖数据

**接口说明**：获取最近历史开奖数据

**接口地址**：`https://www.mxnzp.com/api/lottery/common/history`

**请求方式**：GET

**请求参数**：
| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|----------|------|
| app_id | string | 是 | 应用ID |
| app_secret | string | 是 | 应用密钥 |
| code | string | 是 | 彩票种类标识 |
| size | integer | 否 | 返回数据条数（默认：10） |

**请求示例**：
```
https://www.mxnzp.com/api/lottery/common/history?code=ssq&size=30&app_id=your_app_id&app_secret=your_app_secret
```

**返回示例**：
```json
{
    "code": 1,
    "msg": "数据返回成功",
    "data": [
        {
            "openCode": "01,04,12,13,30,32+08",
            "code": "ssq",
            "expect": "19100",
            "name": "双色球",
            "time": "2019-08-27 21:18:20"
        },
        // ...更多历史数据
    ]
}
```

---

### 4. 获取彩种信息

**接口说明**：获取系统支持的彩种类型详细信息

**接口地址**：`https://www.mxnzp.com/api/lottery/common/types`

**请求方式**：GET

**请求参数**：
| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|----------|------|
| app_id | string | 是 | 应用ID |
| app_secret | string | 是 | 应用密钥 |

**请求示例**：
```
https://www.mxnzp.com/api/lottery/common/types?app_id=your_app_id&app_secret=your_app_secret
```

**返回示例**：
```json
{
    "code": 1,
    "msg": "数据返回成功",
    "data": [
        {
            "typeName": "双色球",
            "typeCode": "ssq",
            "openTime": "每周二、四、日开奖",
            "startTime": "2003年2月16日",
            "ruleDesc": "一等奖（6+1）：浮动。\n二等奖（6+0）：浮动。\n三等奖（5+1）：单注奖金固定为3000元。\n四等奖（5+0、4+1）：单注奖金固定为200元。\n五等奖（4+0、3+1）：单注奖金固定为10元。\n六等奖（2+1、1+1、0+1）：单注奖金固定为5元。"
        },
        // ...更多彩种信息
    ]
}
```

**返回字段说明**：
- `typeName`：彩种名称
- `typeCode`：彩种代码
- `openTime`：开奖时间说明
- `startTime`：开始时间
- `ruleDesc`：中奖规则描述

---

### 5. 中奖结果计算

**接口说明**：根据投注的彩票号码及期数判断是否中奖，暂只支持双色球、大乐透、七乐彩和七星彩；结果根据一定算法进行计算，如有偏差，请以官方为准！

**接口地址**：`https://www.mxnzp.com/api/lottery/common/calc`

**请求方式**：GET

**请求参数**：
| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|----------|------|
| app_id | string | 是 | 应用ID |
| app_secret | string | 是 | 应用密钥 |
| code | string | 是 | 彩票种类标识 |
| expect | string | 是 | 彩票期号 |
| checked_code | string | 是 | 投注号码 |

**号码格式说明**：
- 双色球：`前区号码@后区号码`，例如：`01,03,06,10,11,29@16`
- 大乐透：`前区号码@后区号码`，例如：`13,19,28,30,33@02,12`
- 号码之间用英文逗号分隔，前后区用@分隔

**请求示例**：
```
https://www.mxnzp.com/api/lottery/common/calc?code=cjdlt&expect=19090&checked_code=13,19,28,30,33@02,12&app_id=your_app_id&app_secret=your_app_secret
```

**返回示例**：
```json
{
    "code": 1,
    "msg": "数据返回成功",
    "data": {
        "resultList": [
            {
                "num": "13",
                "lottery": true,
                "blue": false
            },
            // ...更多号码匹配结果
        ],
        "resultDetails": "一等奖，奖金跟随奖池浮动",
        "resultDesc": "5+2",
        "openCode": "13,19,28,30,33+02+12",
        "checkedCode": "13,19,28,30,33@02,12",
        "expect": "19090",
        "code": "cjdlt",
        "codeValue": "超级大乐透"
    }
}
```

**返回字段说明**：
- `resultList`：每个号码的中奖状态列表
- `resultDetails`：中奖结果详情描述
- `resultDesc`：中奖结果简码（如：5+2表示前区中5个，后区中2个）
- `openCode`：开奖号码
- `checkedCode`：投注号码
- `expect`：期号
- `code`：彩票代码
- `codeValue`：彩票名称

---

## 通用说明

### 请求基础信息
- **HOST地址**：`https://www.mxnzp.com/api`
- **请求方法**：所有接口均使用GET请求
- **数据返回格式**：JSON

### 返回数据格式
所有接口都会返回如下标准格式的数据：

```json
{
    "code": 1,
    "msg": "数据返回成功",
    "data": { ... }
}
```

**状态码说明**：
- `code = 1`：请求成功，data字段包含有效数据
- `code = 0`：请求失败，msg字段包含错误信息

### 认证说明
- 所有接口都需要`app_id`和`app_secret`参数进行认证
- 需要在[MXNZP官网](https://www.mxnzp.com)申请专属的应用密钥
- 文档中的示例密钥为临时密钥，仅供测试使用

### 注意事项
1. 接口不限速、不收费，但请合理使用，避免频繁调用
2. 中奖结果计算接口的算法仅供参考，最终结果以官方公布为准
3. 建议在应用中添加缓存机制，减少不必要的API调用
4. 如遇接口变更，请关注官方文档更新

### 支持彩种
目前支持的7大彩种：
1. 双色球（ssq）
2. 大乐透（dlt）
3. 七乐彩（qlc）
4. 福彩3D（fc3d）
5. 七星彩（qxc）
6. 排列3（pl3）
7. 排列5（pl5）
8. 快乐8（kl8）

### 联系方式
- 官方网站：[https://www.mxnzp.com](https://www.mxnzp.com)
- 文档地址：[https://www.mxnzp.com/doc/list](https://www.mxnzp.com/doc/list)
- GitHub：[https://github.com/MZCretin/RollApi](https://github.com/MZCretin/RollApi)

> 注意：本文档内容基于[MXNZP官方文档](https://www.mxnzp.com/doc/detail?id=3)和[掘金文章](https://juejin.cn/post/7057330373985828872)整理，接口细节可能随时间更新，请以官方最新文档为准。