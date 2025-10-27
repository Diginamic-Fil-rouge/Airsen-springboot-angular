export interface ExportDatas{
    commune: {
        inseeCode: string;
        name: string;
        population: number;
        latitude: number;
        longitude: number;
        department: {
            name: string;
            departmentCode: string;
            region: {
                name: string;
                regionCode: string;
            }
        }
    };
    airQuality: {
        measurementDate: string;
        atmIndex: number;
        atmoQual: string;
        atmoColor: string;
        no2: number;
        o3: number;
        pm10: number;
        pm25: number;
        so2: number;
        createdAt: Date;
    };
    weather: {
        observationDate: string;
        temperature: number;
        humidity: number;
        windSpeed: number;
        windDirection: number;
        weatherCode: number;
        createdAt: Date;
    };
    exportMetadata: {
        generatedAt: Date;
        dataFreshness: {
            airQuality: string;
            weather: string;
        }
    }
}