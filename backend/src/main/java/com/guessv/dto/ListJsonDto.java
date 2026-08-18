package com.guessv.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListJsonDto {
    private Meta meta;
    private List<Vtb> vtbs;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        @JsonProperty("UUID_NAMESPACE")
        private String uuidNamespace;
        private long timestamp;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Vtb {
        private String uuid;
        private String type;
        private boolean bot;
        private List<Account> accounts;
        private Name name;
        private String group;
        @JsonProperty("group_name")
        private String groupName;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Account {
        private String id;
        private String type;
        private String platform;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Name {
        private List<String> extra;
        private String cn;
        private String en;
        private String jp;
        @JsonProperty("default")
        private String defaultLang;
    }
}
