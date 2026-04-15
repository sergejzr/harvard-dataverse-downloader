package de.unibn.hrz.dataverse.downloader.model;

import java.util.ArrayList;
import java.util.List;

public class DatasetInfo {
    private long datasetId;
    private String title;
    private String persistentId;
    private String serverUrl;
    private List<DatasetFileEntry> files = new ArrayList<>();

    private String licenseName;
    private String licenseUri;
    private String licenseIconUri;

    private String termsOfUse;
    private String confidentialityDeclaration;
    private String specialPermissions;
    private String restrictions;
    private String citationRequirements;
    private String depositorRequirements;
    private String conditions;
    private String disclaimer;
    private String dataAccessPlace;
    private String originalArchive;
    private String availabilityStatus;
    private String contactForAccess;
    private String sizeOfCollection;
    private String studyCompletion;

    public long getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(long datasetId) {
        this.datasetId = datasetId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPersistentId() {
        return persistentId;
    }

    public void setPersistentId(String persistentId) {
        this.persistentId = persistentId;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public List<DatasetFileEntry> getFiles() {
        return files;
    }

    public void setFiles(List<DatasetFileEntry> files) {
        this.files = files;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
    }

    public String getLicenseUri() {
        return licenseUri;
    }

    public void setLicenseUri(String licenseUri) {
        this.licenseUri = licenseUri;
    }

    public String getLicenseIconUri() {
        return licenseIconUri;
    }

    public void setLicenseIconUri(String licenseIconUri) {
        this.licenseIconUri = licenseIconUri;
    }

    public String getTermsOfUse() {
        return termsOfUse;
    }

    public void setTermsOfUse(String termsOfUse) {
        this.termsOfUse = termsOfUse;
    }

    public String getConfidentialityDeclaration() {
        return confidentialityDeclaration;
    }

    public void setConfidentialityDeclaration(String confidentialityDeclaration) {
        this.confidentialityDeclaration = confidentialityDeclaration;
    }

    public String getSpecialPermissions() {
        return specialPermissions;
    }

    public void setSpecialPermissions(String specialPermissions) {
        this.specialPermissions = specialPermissions;
    }

    public String getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(String restrictions) {
        this.restrictions = restrictions;
    }

    public String getCitationRequirements() {
        return citationRequirements;
    }

    public void setCitationRequirements(String citationRequirements) {
        this.citationRequirements = citationRequirements;
    }

    public String getDepositorRequirements() {
        return depositorRequirements;
    }

    public void setDepositorRequirements(String depositorRequirements) {
        this.depositorRequirements = depositorRequirements;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public String getDataAccessPlace() {
        return dataAccessPlace;
    }

    public void setDataAccessPlace(String dataAccessPlace) {
        this.dataAccessPlace = dataAccessPlace;
    }

    public String getOriginalArchive() {
        return originalArchive;
    }

    public void setOriginalArchive(String originalArchive) {
        this.originalArchive = originalArchive;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public String getContactForAccess() {
        return contactForAccess;
    }

    public void setContactForAccess(String contactForAccess) {
        this.contactForAccess = contactForAccess;
    }

    public String getSizeOfCollection() {
        return sizeOfCollection;
    }

    public void setSizeOfCollection(String sizeOfCollection) {
        this.sizeOfCollection = sizeOfCollection;
    }

    public String getStudyCompletion() {
        return studyCompletion;
    }

    public void setStudyCompletion(String studyCompletion) {
        this.studyCompletion = studyCompletion;
    }

    public boolean hasLicense() {
        return notBlank(licenseName) || notBlank(licenseUri) || notBlank(licenseIconUri);
    }

    public boolean hasCustomTerms() {
        return notBlank(termsOfUse)
                || notBlank(confidentialityDeclaration)
                || notBlank(specialPermissions)
                || notBlank(restrictions)
                || notBlank(citationRequirements)
                || notBlank(depositorRequirements)
                || notBlank(conditions)
                || notBlank(disclaimer)
                || notBlank(dataAccessPlace)
                || notBlank(originalArchive)
                || notBlank(availabilityStatus)
                || notBlank(contactForAccess)
                || notBlank(sizeOfCollection)
                || notBlank(studyCompletion);
    }

    public boolean hasLicenseOrTerms() {
        return hasLicense() || hasCustomTerms();
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}