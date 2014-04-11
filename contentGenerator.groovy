import net.htmlparser.jericho.CharacterReference
import net.htmlparser.jericho.HTMLElementName
import net.htmlparser.jericho.Source
import org.jahia.api.Constants
import org.jahia.registries.ServicesRegistry
import org.jahia.services.SpringContextSingleton
import org.jahia.services.content.JCRCallback
import org.jahia.services.content.JCRNodeWrapper
import org.jahia.services.content.JCRSessionWrapper
import org.jahia.services.content.JCRTemplate
import org.jahia.services.seo.VanityUrl
import org.jahia.services.seo.jcr.VanityUrlManager
import org.jahia.services.usermanager.jcr.JCRGroup

import javax.jcr.RepositoryException

def languagesList = ["sq", "ar", "be", "bg", "ca", "zh", "hr", "cs", "da", "nl", "et", "fi", "fr", "de", "el", "iw", "hu", "is", "in", "it", "ja"];


def nb1stLevelPages = 10
def nb2ndLevelPages = 20
def nb3rdLevelPages = 10
def nbRowPerPages = 2
def nbTextPerCols = 2
def siteName = "mySite"
def nbContents = 0

def percentageExpiration = 20
def percentagePerUser = 33

def aclsOn1stpage = true;
def aclsOn2ndpage = true;
def aclsOn3rdpage = true;
def withExpiration = false;

def nbOfGroupsPerLevel = 5
def randomgroupName = new Random()
def nbUsersPerGroup = 10
def nbUsersOnPlatform = 100

/**
 * Jahia Digital Factory content generator to be run in the Groovy Script Console of the tools section*/

/**
 * Use the lipsum generator to generate Lorem Ipsum dummy paragraphs / words / bytes.
 *
 * Lorem Ipsum courtesy of www.lipsum.com by James Wilson
 *
 * @param what in ['paras','words','bytes'], default: 'paras'
 * @param amount of paras/words/bytes, default: 2 (for words minimum is 5, for bytes it is 27)
 * @param start always start with 'Lorem Ipsum', default = true
 * */
def lipsum = { what = "paras", amount = 2, start = true ->
    def text = new URL("http://www.lipsum.com/feed/xml?what=$what&amount=$amount&start=${start ? 'yes' : 'no'}").text

    def feed = new XmlSlurper().parseText(text)

    feed.lipsum.text()
}

/**
 * Use the randomTextGenerator to generate language specific text.
 * */
def randomTextGeneratorLanguage = ['en', 'cn', 'nl', 'fin', 'fr', 'de', 'el', 'il', 'it', 'jp', 'ltn', 'pl', 'pt', 'ru', 'sr', 'es']

def randomTextGenerator = { language = 'en' ->
    when:
    final URLConnection connection = new URL('http://randomtextgenerator.com/').openConnection()
    connection.setDoOutput(true)
    connection.outputStream.withWriter { Writer writer -> writer << 'text_mode=plain&language=' + language }
    String response = connection.inputStream.withReader { Reader reader -> reader.text }
    then:
    def source = new Source(response)
    CharacterReference.decodeCollapseWhiteSpace(source.getFirstElement(HTMLElementName.TEXTAREA).getContent())
}

/*
assert lipsum() instanceof String
assert lipsum().startsWith('Lorem ipsum')
assert lipsum().split(/\n/).size() == 2
assert lipsum('paras', 10).split(/\n/).size() == 10
assert lipsum(start: false).startsWith('Lorem ipsum')
assert lipsum('words').split(/ /).size() == 5
assert lipsum('bytes').size() == 27
assert lipsum('words',10).split(/ /).size() == 10

println lipsum('paras',10)
*/

/*
println randomGenerator()
println randomGenerator('de')


println randomTextGenerator(randomTextGeneratorLanguage[rand.nextInt(randomTextGeneratorLanguage.size())])
*/

def rand = new Random();

def addExpiration = { JCRNodeWrapper node ->
    if (withExpiration) {
        if (nbContents % percentageExpiration == 0) {
            node.addMixin("jmix:cache")
            node.setProperty("j:expiration", rand.nextInt(10))
        }
        if ((nbContents) % percentagePerUser == 0) {
            node.addMixin("jmix:cache")
            node.setProperty("j:perUser", true)
            node.setProperty("j:expiration", rand.nextInt(5))
        }
    }
}
def rolesDefinition = [new HashSet<String>(["editor-in-chief"]), new HashSet<String>(["editor"]), new HashSet<String>(["owner"]), new HashSet<String>(["reviewer"]), new HashSet<String>(["contributor"])]
def addAcl = { JCRNodeWrapper node, groupName -> node.grantRoles(groupName, rolesDefinition[rand.nextInt(rolesDefinition.size())]) }



def createContent = { JCRNodeWrapper page, int nbRow, int nbText ->
    println "Create content for " + page.getPath()
    def random = new Random();
    JCRNodeWrapper area = page.getNode("pagecontent")
    JCRNodeWrapper row = area.getNode("bootstrap-row");
    (1..nbRow).each {
        row.copy(area, "bootstrap-row" + it, false)
        nbContents++;
        JCRNodeWrapper newRow = area.getNode("bootstrap-row" + it);
        JCRNodeWrapper list = newRow.getNode("bootstrap-column").addNode("bootstrap-column", "jnt:contentList");
        (1..nbText).each { idx ->
            nbContents++;
            def i = random.nextInt(randomTextGeneratorLanguage.size())
            def newNode = list.addNode("simple-text" + idx, "jnt:text")
            try {
                //newNode.setProperty("text", randomTextGenerator(randomTextGeneratorLanguage[i]).toString())
                newNode.setProperty("text", "" + lipsum('paras', 2, false).toString())
                addExpiration(newNode)
            } catch (Exception e) {}
        }
        list = newRow.getNode("bootstrap-column-1").addNode("bootstrap-column-1", "jnt:contentList");
        (1..nbText).each { idx ->
            nbContents++;
            def newNode = list.addNode("simple-text" + idx, "jnt:text")
            try {
                //newNode.setProperty("text", randomTextGenerator(randomTextGeneratorLanguage[i]).toString())
                newNode.setProperty("text", "" + lipsum('paras', 2, false).toString())
                addExpiration(newNode)
            } catch (Exception e) {}
        }
    }

}

def updateContent = { JCRNodeWrapper page, int nbRow, int nbText, locale ->
    def random = new Random();
    JCRNodeWrapper area = page.getNode("pagecontent")
    (1..nbRow).each {
        try {
            JCRNodeWrapper newRow = area.getNode("bootstrap-row" + it);
            JCRNodeWrapper list = newRow.getNode("bootstrap-column").getNode("bootstrap-column");
            (1..nbText).each { idx ->
                def i = random.nextInt(randomTextGeneratorLanguage.size())
                try {
                    list.getNode("simple-text" + idx).setProperty("text",
                            locale + " " + randomTextGenerator(randomTextGeneratorLanguage[i]))
                } catch (Exception e) {}
            }
            list = newRow.getNode("bootstrap-column-1").getNode("bootstrap-column-1");
            (1..nbText).each { idx ->
                try {
                    list.getNode("simple-text" + idx).setProperty("text", locale + " " + lipsum('paras', 2, false).toString())
                } catch (Exception e) {}
            }
        } catch (Exception e) {}
    }

}

def VanityUrlManager vanityUrlManager = (VanityUrlManager) SpringContextSingleton.getBean("org.jahia.services.seo.jcr.VanityUrlManager")

def generateContent = {
    nbPagesFirstLevel, nbPagesSecondLevel, nbPagesThirdLevel, nbRowPerPage, nbTextPerPage, randomElements = false, publishFirstLevelOnEachIteration = false ->
        JCRTemplate.getInstance().doExecuteWithSystemSession(null, Constants.EDIT_WORKSPACE, Locale.ENGLISH,
                new JCRCallback<Object>() {
                    public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                        def randomEl = new Random();
                        JCRNodeWrapper home = session.getNode("/sites/" + siteName + "/home");
                        JCRNodeWrapper page1 = home.getNode("page");
                        def int nbPage = 0;

                        (1..nbPagesFirstLevel).each { it1 ->
                            if (page1.copy(home, "page" + (++nbPage), false)) {
                                nbContents++;
                                JCRNodeWrapper pageLvl1 = home.getNode("page" + (nbPage));
                                pageLvl1.setProperty("jcr:title", "Page number " + (nbPage))
                                vanityUrlManager.saveVanityUrlMapping(pageLvl1, new VanityUrl("/pages" + siteName + "/page_en_" + nbPage, siteName, "en", true, true), session)
                                def groupName = "g:group_" + randomgroupName.nextInt(nbOfGroupsPerLevel + 1)
                                if (aclsOn1stpage) {
                                    addAcl(pageLvl1, groupName)
                                }
                                println "level 1" + pageLvl1.getPath()
                                (1..nbPagesSecondLevel).each { it2 ->
                                    if (page1.copy(pageLvl1, "page" + (++nbPage), false)) {
                                        nbContents++;
                                        JCRNodeWrapper pageLvl2 = pageLvl1.getNode("page" + nbPage);
                                        pageLvl2.setProperty("jcr:title", "Page number " + (nbPage))
                                        vanityUrlManager.saveVanityUrlMapping(pageLvl2, new VanityUrl("/pages" + siteName + "/page_en_" + nbPage, siteName, "en", true, true), session)
                                        def subgroupName = groupName + "_" + randomgroupName.nextInt(nbOfGroupsPerLevel + 1)
                                        if (aclsOn2ndpage) {
                                            addAcl(pageLvl2, subgroupName)
                                        }
                                        println "level 2" + pageLvl2.getPath()
                                        (1..nbPagesThirdLevel).each { it3 ->
                                            if (page1.copy(pageLvl2, "page" + (++nbPage), false)) {
                                                nbContents++;
                                                JCRNodeWrapper pageLvl3 = pageLvl2.getNode("page" + nbPage);
                                                pageLvl3.setProperty("jcr:title", "Page number " + (nbPage))
                                                vanityUrlManager.saveVanityUrlMapping(pageLvl3, new VanityUrl("/pages" + siteName + "/page_en_" + nbPage, siteName, "en", true, true), session)
                                                def subsubgroupName = subgroupName + "_" + randomgroupName.nextInt(nbOfGroupsPerLevel + 1)
                                                if (aclsOn3rdpage) {
                                                    addAcl(pageLvl3, subsubgroupName)
                                                }
                                                println "level 3" + pageLvl3.getPath()
                                                createContent(pageLvl3, (randomElements ? randomEl.nextInt(nbRowPerPage) : nbRowPerPage),
                                                        (randomElements ? randomEl.nextInt(nbTextPerPage) : nbTextPerPage));
                                                session.save();
                                            }
                                        }
                                        createContent(pageLvl2,
                                                (randomElements ? randomEl.nextInt(nbRowPerPage) : nbRowPerPage),
                                                (randomElements ? randomEl.nextInt(nbTextPerPage) : nbTextPerPage));
                                        session.save();
                                    }

                                }
                                createContent(pageLvl1,
                                        (randomElements ? randomEl.nextInt(nbRowPerPage) : nbRowPerPage),
                                        (randomElements ? randomEl.nextInt(nbTextPerPage) : nbTextPerPage));
                                session.save();
                                ServicesRegistry.instance.JCRPublicationService.publishByMainId(pageLvl1.identifier)
                            }
                        }
                        return null;
                    }
                });
}


def updateContentForLanguages = {
    nbPagesFirstLevel, nbPagesSecondLevel, nbPagesThirdLevel, nbRowPerPage, nbTextPerPage, languages = languagesList ->
        languages.each { language ->
            locale = new java.util.Locale(language);
            JCRTemplate.getInstance().doExecuteWithSystemSession(null, Constants.EDIT_WORKSPACE, locale,
                    new JCRCallback<Object>() {
                        public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                            println session.locale.displayName;
                            JCRNodeWrapper home = session.getNode("/sites/" + siteName + "/home");
                            def int nbPage = 1;
                            (1..nbPagesFirstLevel).each { it1 ->
                                JCRNodeWrapper pageLvl1 = home.getNode("page" + nbPage);
                                if (!pageLvl1.hasNode("j:translation_" + session.locale)) {
                                    pageLvl1.setProperty("jcr:title",
                                            "Page number " + (nbPage++) + " " + session.locale.getDisplayName(session.locale))
                                    vanityUrlManager.saveVanityUrlMapping(pageLvl1, new VanityUrl("/pages/page_" + session.locale + "_" + nbPage, siteName, session.locale.toString(), true, true), session)
                                    (1..nbPagesSecondLevel).each { it2 ->
                                        JCRNodeWrapper pageLvl2 = pageLvl1.getNode("page" + nbPage);
                                        if (!pageLvl2.hasNode("j:translation_" + session.locale)) {
                                            pageLvl2.setProperty("jcr:title",
                                                    "Page number " + (nbPage++) + " " + session.locale.getDisplayName(session.locale))
                                            vanityUrlManager.saveVanityUrlMapping(pageLvl2, new VanityUrl("/pages/page_" + session.locale + "_" + nbPage, siteName, session.locale.toString(), true, true), session)
                                            (1..nbPagesThirdLevel).each { it3 ->
                                                JCRNodeWrapper pageLvl3 = pageLvl2.getNode("page" + nbPage);
                                                if (!pageLvl3.hasNode("j:translation_" + session.locale)) {
                                                    pageLvl3.setProperty("jcr:title", "Page number " + (nbPage++) + " " + session.locale.getDisplayName(session.locale))
                                                    vanityUrlManager.saveVanityUrlMapping(pageLvl3, new VanityUrl("/pages/page_" + session.locale + "_" + nbPage, siteName, session.locale.toString(), true, true), session)
                                                    updateContent(pageLvl3, nbRowPerPage, nbTextPerPage,
                                                            session.locale.displayName);
                                                    session.save();
                                                }
                                            }
                                            updateContent(pageLvl2, nbRowPerPage, nbTextPerPage, session.locale.displayName);
                                            session.save();
                                        }
                                    }
                                    updateContent(pageLvl1, nbRowPerPage, nbTextPerPage, session.locale.displayName);
                                    session.save();
                                    ServicesRegistry.instance.JCRPublicationService.publishByMainId(pageLvl1.identifier);
                                }
                            }
                            return null;
                        }
                    });
        }
}

def createGroups = {
    def randUsers = new Random()
    def service = ServicesRegistry.instance.jahiaGroupManagerService
    def userService = ServicesRegistry.instance.jahiaUserManagerService
    if (service.lookupGroup(siteName, "group_0_0_0") == null) {
        (0..nbOfGroupsPerLevel).each { first ->
            (0..nbOfGroupsPerLevel).each { second ->
                (0..nbOfGroupsPerLevel).each {
                    def JCRGroup group = (JCRGroup) service.createGroup(siteName,
                            "group_" + first + "_" + second + "_" + it, null,
                            false)
                    (0..nbUsersPerGroup).each {
                        group.addMember(userService.lookupUser("user" + (randUsers.nextInt(nbUsersOnPlatform + 1) + 1)))
                    }
                }
            }
        }
        (0..nbOfGroupsPerLevel).each { first ->
            (0..nbOfGroupsPerLevel).each { second ->
                def JCRGroup group = (JCRGroup) service.createGroup(siteName, "group_" + first + "_" + second, null,
                        false)
                (0..3).each {
                    group.addMember(service.lookupGroup(siteName,
                            "group_" + first + "_" + second + "_" + randUsers.nextInt(nbOfGroupsPerLevel + 1)))
                }
            }
        }
        (0..nbOfGroupsPerLevel).each { first ->
            def JCRGroup group = (JCRGroup) service.createGroup(siteName, "group_" + first, null, false)
            (0..3).each {
                group.addMember(service.lookupGroup(siteName, "group_" + first + "_" + randUsers.nextInt(nbOfGroupsPerLevel + 1)))
            }
        }
    }
}

createGroups()
//generateContent(nb1stLevelPages, nb2ndLevelPages, nb3rdLevelPages, nbRowPerPages, nbTextPerCols, true, true)

// Multilingual system

generateContent(nb1stLevelPages, nb2ndLevelPages, nb3rdLevelPages, nbRowPerPages, nbTextPerCols, false)
updateContentForLanguages(nb1stLevelPages, nb2ndLevelPages, nb3rdLevelPages, nbRowPerPages, nbTextPerCols, languagesList)

//updateContentForLanguages(nb1stLevelPages, nb2ndLevelPages, nb3rdLevelPages, nbRowPerPages, nbTextPerCols, languagesList[0..2])

/*
JCRTemplate.instance.doExecuteWithSystemSession(new JCRCallback() {Object doInJCR(JCRSessionWrapper session) throws javax.jcr.RepositoryException {
    QueryResult  queryResult = session.workspace.queryManager.createQuery("SELECT * FROM [jmix:cache] as cont where isdescendantnode('/sites/mySite')",javax.jcr.query.Query.JCR_SQL2).execute();
    NodeIterator iterator = queryResult.getNodes();
    while (iterator.hasNext()){
        javax.jcr.Node node = iterator.nextNode();
        node.setProperty("j:expiration",0);
    }
    session.save();
    return null  //To change body of implemented methods use File | Settings | File Templates.
}
});*/
