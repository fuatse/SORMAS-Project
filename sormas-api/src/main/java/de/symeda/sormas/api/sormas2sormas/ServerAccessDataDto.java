package de.symeda.sormas.api.sormas2sormas;

import java.util.Objects;

public class ServerAccessDataDto {

    private String commonName;
    private String organization;
    private String url;

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerAccessDataDto that = (ServerAccessDataDto) o;
        return Objects.equals(commonName, that.commonName) &&
                Objects.equals(organization, that.organization) &&
                Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commonName, organization, url);
    }

    @Override
    public String toString() {
        return "ServerAccessDataDto{" +
                "commonName='" + commonName + '\'' +
                ", organization='" + organization + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
