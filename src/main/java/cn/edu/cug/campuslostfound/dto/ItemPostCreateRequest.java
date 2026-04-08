package cn.edu.cug.campuslostfound.dto;

// 接收前端传入数据的类，在后端中会被重构为 ItemPost 类
public class ItemPostCreateRequest
{
    private String type;
    private String title;
    private String description;
    private String contact;
    private String location;
    private String imageUrl;
    private String itemStatus;
    private String incidentTime;
    private String publisherId;
    private Double latitude;
    private Double longitude;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getItemStatus() { return itemStatus; }
    public void setItemStatus(String itemStatus) { this.itemStatus = itemStatus; }

    public String getIncidentTime() { return incidentTime; }
    public void setIncidentTime(String incidentTime) { this.incidentTime = incidentTime; }

    public String getPublisherId() { return publisherId; }
    public void setPublisherId(String publisherId) { this.publisherId = publisherId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
