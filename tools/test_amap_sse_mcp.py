from __future__ import annotations

import unittest

from tools.amap_sse_mcp import extract_poi_results, extract_weather_result, require_api_key


class AmapSseMcpTest(unittest.TestCase):

    def test_extract_weather_result_returns_compact_fields(self) -> None:
        payload = {
            "status": "1",
            "lives": [
                {
                    "province": "上海市",
                    "city": "上海市",
                    "adcode": "310000",
                    "weather": "晴",
                    "temperature": "26",
                    "winddirection": "东南",
                    "windpower": "3",
                    "humidity": "40",
                    "reporttime": "2026-03-21 12:00:00",
                }
            ],
        }

        result = extract_weather_result(payload)

        self.assertEqual(result["city"], "上海市")
        self.assertEqual(result["weather"], "晴")
        self.assertEqual(result["temperature"], "26℃")
        self.assertEqual(result["wind"], "东南风 3级")
        self.assertEqual(result["humidity"], "40%")
        self.assertEqual(result["report_time"], "2026-03-21 12:00:00")

    def test_extract_poi_results_limits_and_shapes_fields(self) -> None:
        payload = {
            "status": "1",
            "pois": [
                {
                    "name": "Manner Coffee",
                    "address": "静安区南京西路100号",
                    "type": "餐饮服务;咖啡厅",
                    "location": "121.4737,31.2304",
                    "distance": "120",
                },
                {
                    "name": "Seesaw Coffee",
                    "address": "静安区铜仁路88号",
                    "type": "餐饮服务;咖啡厅",
                    "location": "121.4740,31.2310",
                    "distance": "260",
                },
                {
                    "name": "Blue Bottle",
                    "address": "静安区某路66号",
                    "type": "餐饮服务;咖啡厅",
                    "location": "121.4750,31.2320",
                    "distance": "390",
                },
            ],
        }

        result = extract_poi_results(payload, limit=2)

        self.assertEqual(len(result), 2)
        self.assertEqual(result[0]["name"], "Manner Coffee")
        self.assertEqual(result[0]["distance"], "120米")
        self.assertEqual(result[1]["address"], "静安区铜仁路88号")

    def test_require_api_key_rejects_blank_value(self) -> None:
        with self.assertRaises(ValueError):
            require_api_key("")


if __name__ == "__main__":
    unittest.main()
