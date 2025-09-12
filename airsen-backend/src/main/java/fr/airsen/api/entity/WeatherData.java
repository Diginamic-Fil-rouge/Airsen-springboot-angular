package fr.airsen.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDate;
import java.util.Objects;

@Entity
public class WeatherData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Commune commune;

    private LocalDate measurementDate;

    private Integer atmIndex;

    private String atmoQual;

    private String atmoColor;

    private double NO2;

    private double O3;

    private double Pm10;

    private Integer Pm25;

    private double SO2;

    private LocalDate createdAt;

    public WeatherData() {
    }

    public WeatherData(Commune commune, LocalDate measurementDate, Integer atmIndex, String atmoQual, String atmoColor, double NO2, double o3, double pm10, Integer pm25, double SO2, LocalDate createdAt) {
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

    public Integer getId() {
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
        if (!(o instanceof WeatherData that)) return false;
        return Double.compare(NO2, that.NO2) == 0 && Double.compare(O3, that.O3) == 0 && Double.compare(Pm10, that.Pm10) == 0 && Double.compare(SO2, that.SO2) == 0 && Objects.equals(commune, that.commune) && Objects.equals(measurementDate, that.measurementDate) && Objects.equals(atmIndex, that.atmIndex) && Objects.equals(atmoQual, that.atmoQual) && Objects.equals(atmoColor, that.atmoColor) && Objects.equals(Pm25, that.Pm25);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commune, measurementDate, atmIndex, atmoQual, atmoColor, NO2, O3, Pm10, Pm25, SO2);
    }
}
