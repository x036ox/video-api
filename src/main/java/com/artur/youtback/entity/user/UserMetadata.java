package com.artur.youtback.entity.user;

import com.artur.youtback.utils.MapUtils;
import jakarta.persistence.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
public class UserMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @OneToOne(mappedBy = "userMetadata")
    @MapsId
    @JoinColumn(name = "id")
    private UserEntity userEntity;

    @ElementCollection
    @CollectionTable(
            name = "user_language",
            joinColumns = @JoinColumn(name = "metadata_id")
    )
    @MapKeyColumn(name = "language")
    @Column(name = "repeats")
    private Map<String,Integer> languages = new HashMap<>();


    @ElementCollection
    @CollectionTable(
            name = "user_category",
            joinColumns = @JoinColumn(name = "metadata_id")
    )
    @MapKeyColumn(name = "category")
    @Column(name = "repeats")
    private Map<String, Integer> categories = new HashMap<>();


    public UserMetadata(UserEntity userEntity) {
        this();
        this.userEntity = userEntity;
    }

    public UserMetadata() {
    }

    public void incrementLanguage(String category){
        this.languages.compute(category, (key,value) -> value != null ? value + 1 : 1);
    }

    public void incrementCategory(String category){
        this.categories.compute(category, (key,value) -> value != null ? value + 1 : 1);
    }

    public Map<String, Integer> getCategories() {
        return this.categories;
    }

    public void setCategories(Map<String,Integer> categories) {
        this.categories = categories;
    }


    public LinkedHashMap<String, Integer> getLanguagesDec() {
        return MapUtils.sortMapByValueDec(this.languages);
    }

    public LinkedHashMap<String, Integer> getCategoriesDec() {
        return MapUtils.sortMapByValueDec(this.categories);
    }


    public Map<String, Integer> getLanguages() {
        return this.languages;
    }

    public void setLanguages(Map<String, Integer> languages) {
        this.languages = languages;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UserEntity getUserEntity() {
        return userEntity;
    }

    public void setUserEntity(UserEntity userEntity) {
        this.userEntity = userEntity;
    }


}
