-- Migration 68 -> 69: Remove legacy API version support
-- Migrate old API versions to auto (0)
-- Old values: -1 (html), -2 (v1_14), -3 (v1_15), -4 (v1_19_1), -5 (v1_23)
-- Keep: 0 (auto), -6 (v1_32), -7 (v1_40), -8 (v1_50)
UPDATE profiles SET api_version = 0 WHERE api_version IN (-1, -2, -3, -4, -5);
