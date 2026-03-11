package com.fineasy.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "dart_corp_code", indexes = {
        @Index(name = "idx_dart_corp_stock_code", columnList = "stock_code", unique = true)
})
public class DartCorpCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "corp_code", nullable = false, unique = true, length = 20)
    private String corpCode;

    @Column(name = "corp_name", nullable = false, length = 200)
    private String corpName;

    @Column(name = "stock_code", nullable = false, unique = true, length = 20)
    private String stockCode;

    protected DartCorpCodeEntity() {
    }

    public DartCorpCodeEntity(String corpCode, String corpName, String stockCode) {
        this.corpCode = corpCode;
        this.corpName = corpName;
        this.stockCode = stockCode;
    }

    public Long getId() { return id; }
    public String getCorpCode() { return corpCode; }
    public String getCorpName() { return corpName; }
    public String getStockCode() { return stockCode; }
}
