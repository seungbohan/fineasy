package com.fineasy.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "macro_indicators", indexes = {
        @Index(name = "idx_macro_code_date",
                columnList = "indicator_code, record_date DESC")
})
public class MacroIndicatorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "indicator_code", nullable = false, length = 50)
    private String indicatorCode;

    @Column(name = "indicator_name", length = 100)
    private String indicatorName;

    @Column(name = "indicator_value", nullable = false)
    private Double value;

    @Column(length = 30)
    private String unit;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(length = 100)
    private String source;

    protected MacroIndicatorEntity() {
    }

    public MacroIndicatorEntity(Long id, String indicatorCode, String indicatorName,
                                Double value, String unit, LocalDate recordDate, String source) {
        this.id = id;
        this.indicatorCode = indicatorCode;
        this.indicatorName = indicatorName;
        this.value = value;
        this.unit = unit;
        this.recordDate = recordDate;
        this.source = source;
    }

    public Long getId() { return id; }
    public String getIndicatorCode() { return indicatorCode; }
    public String getIndicatorName() { return indicatorName; }
    public Double getValue() { return value; }
    public String getUnit() { return unit; }
    public LocalDate getRecordDate() { return recordDate; }
    public String getSource() { return source; }
}
