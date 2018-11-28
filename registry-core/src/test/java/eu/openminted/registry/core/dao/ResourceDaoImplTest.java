package eu.openminted.registry.core.dao;

import configuration.MockDatabaseConfiguration;
import eu.openminted.registry.core.domain.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.transaction.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
            MockDatabaseConfiguration.class
        })
@Transactional
public class ResourceDaoImplTest {

    private static Logger logger = LogManager.getLogger(ResourceDaoImplTest.class);

    @Autowired
    ResourceDao resourceDao;

    private Resource resource;



    @Before
    public void iniatiateResource(){
       logger.info("HELLOOOOOO");
    }


    @After
    public void after(){
        logger.info("OMAGAD");
    }

    @Test
    public void getResource() {

        Resource resource = new Resource();

    }

//    @Test
//    public void getModifiedSince() {
//    }
//
//    @Test
//    public void getModifiedSince1() {
//    }
//
//    @Test
//    public void getCreatedSince() {
//    }
//
//    @Test
//    public void getCreatedSince1() {
//    }
//
//    @Test
//    public void getResource1() {
//    }
//
//    @Test
//    public void getResourceStream() {
//    }
//
//    @Test
//    public void getResource2() {
//    }
//
//    @Test
//    public void getResource3() {
//    }
//
//    @Test
//    public void getResource4() {
//    }
//
//    @Test
//    public void addResource() {
//    }
//
//    @Test
//    public void updateResource() {
//    }
//
//    @Test
//    public void deleteResource() {
//    }
}