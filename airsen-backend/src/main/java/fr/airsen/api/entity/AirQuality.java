package fr.airsen.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Entity representing the air quality for a given commune.
 */
@Entity
@Table(name = "air_quality")
public class AirQuality {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "commune_id")
    private Commune commune;

    @Column(name = "measurement_date")
    private LocalDate measurementDate;

    @Column(name = "atm_index")
    @Min(value = 1, message = "The ATM index must be at least 1")
    private Integer atmIndex;

    @Column(name = "atmo_qual")
    private String atmoQual;

    @Column(name = "atmo_color")
    @Size(min = 7, max = 7, message = "The ATM color must be a valid hex code (e.g., #50ccaa)")
    private String atmoColor;

    @Column(name = "no2")
    @Min(value = 0, message = "The NO2 must be greater than or equal to 0")
    private double NO2;

    @Column(name = "o3")
    @Min(value = 0, message = "The O3 must be greater than or equal to 0")
    private double O3;

    @Column(name = "pm10")
    @Min(value = 0, message = "The PM10 must be greater than or equal to 0")
    private double Pm10;

    @Column(name = "pm25")
    @Min(value = 0, message = "The PM25 must be greater than or equal to 0")
    private Integer Pm25;

    @Column(name = "so2")
    @Min(value = 0, message = "The SO2 must be greater than or equal to 0")
    private double SO2;

    @Column(name = "created_at")
    @PastOrPresent(message = "The created at date must be in the past or present")
    private LocalDate createdAt;

    /**
     * Alert histories triggered by this air quality measurement.
     */
    @OneToMany(mappedBy = "airQuality", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.List<AlertHistory> alertHistories;

    public AirQuality() {
    }

    public AirQuality(Commune commune, LocalDate measurementDate, Integer atmIndex, String atmoQual, String atmoColor, double NO2, double o3, double pm10, Integer pm25, double SO2, LocalDate createdAt) {
        this.commune = commune;
        this.measurementDate = measurementDate;
        this.atmIndex = atmIndex;
        this.atmoQual = atmoQual;
        this.atmoColor = atmoColor;
        this.NO2 = NO2;
        O3 = o3;
        Pm10 = pm10;
        Pm25 = pm25;
        this.SO2 = SO2;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Commune getCommune() {
        return commune;
    }

    public void setCommune(Commune commune) {
        this.commune = commune;
    }

    public LocalDate getMeasurementDate() {
        return measurementDate;
    }

    public void setMeasurementDate(LocalDate measurementDate) {
        this.measurementDate = measurementDate;
    }

    public Integer getAtmIndex() {
        return atmIndex;
    }

    public void setAtmIndex(Integer atmIndex) {
        this.atmIndex = atmIndex;
    }

    public String getAtmoQual() {
        return atmoQual;
    }

    public void setAtmoQual(String atmoQual) {
        this.atmoQual = atmoQual;
    }

    public String getAtmoColor() {
        return atmoColor;
    }

    public void setAtmoColor(String atmoColor) {
        this.atmoColor = atmoColor;
    }

    public double getNO2() {
        return NO2;
    }

    public void setNO2(double NO2) {
        this.NO2 = NO2;
    }

    public double getO3() {
        return O3;
    }

    public void setO3(double o3) {
        O3 = o3;
    }

    public double getPm10() {
        return Pm10;
    }

    public void setPm10(double pm10) {
        Pm10 = pm10;
    }

    public Integer getPm25() {
        return Pm25;
    }

    public void setPm25(Integer pm25) {
        Pm25 = pm25;
    }

    public double getSO2() {
        return SO2;
    }

    public void setSO2(double SO2) {
        this.SO2 = SO2;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public java.util.List<AlertHistory> getAlertHistories() {
        return alertHistories;
    }

    public void setAlertHistories(java.util.List<AlertHistory> alertHistories) {
        this.alertHistories = alertHistories;
    }

    // External API integration methods (aliases for compatibility)
    public Integer getAtmoIndex() {
        return atmIndex;
    }

    public void setAtmoIndex(Integer atmoIndex) {
        this.atmIndex = atmoIndex;
    }

    public String getQualifier() {
        return atmoQual;
    }

    public void setQualifier(String qualifier) {
        this.atmoQual = qualifier;
    }

    public String getColor() {
        return atmoColor;
    }

    public void setColor(String color) {
        this.atmoColor = color;
    }

    public Double getNo2Concentration() {
        return NO2;
    }

    public void setNo2Concentration(Double no2) {
        this.NO2 = no2 != null ? no2 : 0.0;
    }

    public Double getO3Concentration() {
        return O3;
    }

    public void setO3Concentration(Double o3) {
        this.O3 = o3 != null ? o3 : 0.0;
    }

    public Double getPm10Concentration() {
        return (double) Pm10;
    }

    public void setPm10Concentration(Double pm10) {
        this.Pm10 = pm10 != null ? pm10 : 0.0;
    }

    public Double getPm25Concentration() {
        return Pm25 != null ? Pm25.doubleValue() : 0.0;
    }

    public void setPm25Concentration(Double pm25) {
        this.Pm25 = pm25 != null ? pm25.intValue() : 0;
    }

    public Double getSo2Concentration() {
        return SO2;
    }

    public void setSo2Concentration(Double so2) {
        this.SO2 = so2 != null ? so2 : 0.0;
    }

    @Override
    public String toString() {
        return "WeatherData{" +
                "id=" + id +
                ", commune=" + commune +
                ", measurementDate=" + measurementDate +
                ", atmIndex=" + atmIndex +
                ", atmoQual='" + atmoQual + '\'' +
                ", atmoColor='" + atmoColor + '\'' +
                ", NO2=" + NO2 +
                ", O3=" + O3 +
                ", Pm10=" + Pm10 +
                ", Pm25=" + Pm25 +
                ", SO2=" + SO2 +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AirQuality that)) return false;
        return Double.compare(NO2, that.NO2) == 0 && Double.compare(O3, that.O3) == 0 && Double.compare(Pm10, that.Pm10) == 0 && Double.compare(SO2, that.SO2) == 0 && Objects.equals(commune, that.commune) && Objects.equals(measurementDate, that.measurementDate) && Objects.equals(atmIndex, that.atmIndex) && Objects.equals(atmoQual, that.atmoQual) && Objects.equals(atmoColor, that.atmoColor) && Objects.equals(Pm25, that.Pm25);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commune, measurementDate, atmIndex, atmoQual, atmoColor, NO2, O3, Pm10, Pm25, SO2);
    }
}
