/*
 * Copyright © 2024 Damyan Ivanov.
 * This file is part of MoLe.
 * MoLe is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * MoLe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoLe. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger.json.v1_50;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the adata field structure in hledger-web 1.50+.
 *
 * JSON structure:
 * {
 *   "pdperiods": [["0000-01-01", { "bdincludingsubs": [...], "bdexcludingsubs": [...], "bdnumpostings": 1 }]],
 *   "pdpre": { "bdincludingsubs": [], "bdexcludingsubs": [], "bdnumpostings": 0 }
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedAccountData {
    @JsonDeserialize(using = PdPeriodsDeserializer.class)
    private List<PeriodEntry> pdperiods;
    private ParsedBalanceData pdpre;

    public ParsedAccountData() {
    }

    public List<PeriodEntry> getPdperiods() {
        return pdperiods;
    }

    public void setPdperiods(List<PeriodEntry> pdperiods) {
        this.pdperiods = pdperiods;
    }

    public ParsedBalanceData getPdpre() {
        return pdpre;
    }

    public void setPdpre(ParsedBalanceData pdpre) {
        this.pdpre = pdpre;
    }

    /**
     * Get the balance data from the first period entry.
     * In typical usage, there's usually one period entry with date "0000-01-01".
     */
    public ParsedBalanceData getFirstPeriodBalance() {
        if (pdperiods != null && !pdperiods.isEmpty()) {
            return pdperiods.get(0).getBalanceData();
        }
        return null;
    }

    /**
     * Represents a single period entry: [date, balanceData]
     */
    public static class PeriodEntry {
        private String date;
        private ParsedBalanceData balanceData;

        public PeriodEntry() {
        }

        public PeriodEntry(String date, ParsedBalanceData balanceData) {
            this.date = date;
            this.balanceData = balanceData;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public ParsedBalanceData getBalanceData() {
            return balanceData;
        }

        public void setBalanceData(ParsedBalanceData balanceData) {
            this.balanceData = balanceData;
        }
    }

    /**
     * Custom deserializer for pdperiods which is a heterogeneous array:
     * [["0000-01-01", { balanceData }], ...]
     */
    public static class PdPeriodsDeserializer extends JsonDeserializer<List<PeriodEntry>> {
        @Override
        public List<PeriodEntry> deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            List<PeriodEntry> result = new ArrayList<>();
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            JsonNode arrayNode = mapper.readTree(p);

            if (arrayNode.isArray()) {
                for (JsonNode entryNode : arrayNode) {
                    if (entryNode.isArray() && entryNode.size() >= 2) {
                        String date = entryNode.get(0).asText();
                        ParsedBalanceData balanceData = mapper.treeToValue(
                                entryNode.get(1), ParsedBalanceData.class);
                        result.add(new PeriodEntry(date, balanceData));
                    }
                }
            }

            return result;
        }
    }
}
