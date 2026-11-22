package com.example.mapservice.service;

import com.example.mapservice.dto.PlaceResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GazetteerTest {

    @Test
    void searchIgnoresCaseAndAccents() {
        List<PlaceResult> places = Gazetteer.search("BEJA");
        assertTrue(places.stream().anyMatch(p -> p.label().startsWith("Béja")));
    }

    @Test
    void searchReturnsAtMostSixResults() {
        assertTrue(Gazetteer.search("tunisie").size() <= 6);
    }

    @Test
    void searchReturnsNothingForUnknownPlace() {
        assertTrue(Gazetteer.search("zzzzzz").isEmpty());
    }
}
