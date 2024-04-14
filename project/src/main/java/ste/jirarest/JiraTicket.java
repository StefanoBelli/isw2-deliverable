package ste.jirarest;

import org.json.JSONObject;

import ste.jirarest.util.Http;
import ste.jirarest.util.Parsing;

public final class JiraTicket {
    public final class Fields {
        public final class Version {
            private final String self;
            private final String id;
            private final String description;
            private final String name;
            private final boolean archived;
            private final boolean released;
            private final String releaseDate;
            
            private Version(JSONObject o) {
                self = o.getString("self");
                id = o.getString("id");
                description = o.getString("description");
                name = o.getString("name");
                archived = o.getBoolean("archived");
                released = o.getBoolean("released");
                releaseDate = o.getString("releaseDate");
            }

            public String getDescription() {
                return description;
            }

            public String getId() {
                return id;
            }

            public String getName() {
                return name;
            }

            public String getReleaseDate() {
                return releaseDate;
            }
            
            public String getSelf() {
                return self;
            }

            public boolean isArchived() {
                return archived;
            }

            public boolean isReleased() {
                return released;
            }
        }

        private final Version[] versions;
        private final String resolutionDate;
        private final String created;

        private Fields(JSONObject o) {
            versions = (Version[]) Parsing.getArray(o.getJSONArray("versions"), Version.class);
            resolutionDate = o.getString("resolutionDate");
            created = o.getString("created");
        }

        public Version[] getVersions() {
            return versions;
        }

        public String getCreated() {
            return created;
        }

        public String getResolutionDate() {
            return resolutionDate;
        }
    }

    private final String expand;
    private final String id;
    private final String self;
    private final String key;
    private final Fields fields;

    private JiraTicket(JSONObject o) {
        expand = o.getString("expand");
        id = o.getString("id");
        self = o.getString("self");
        key = o.getString("key");
        fields = new Fields(o.getJSONObject("fields"));
    }

    public String getExpand() {
        return expand;
    }

    public Fields getFields() {
        return fields;
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getSelf() {
        return self;
    }

    private static final class JiraTicketHeader {
        /*private final String expand;
        private final int startAt;
        private final int maxResults;*/
        private final int total;

        private JiraTicketHeader(JSONObject o) {
            /*expand = o.getString("expand");
            startAt = o.getInt("startAt");
            maxResults = o.getInt("maxResults");*/
            total = o.getInt("total");
        }

        /*
        public String getExpand() {
            return expand;
        }

        public int getMaxResults() {
            return maxResults;
        }

        public int getStartAt() {
            return startAt;
        }
        */

        public int getTotal() {
            return total;
        }
    }

    public static JiraTicket[] getAllTicketsByName(String name) throws Http.RequestException {
        StringBuilder origBuilder = new StringBuilder();
        origBuilder
            .append("https://issues.apache.org/jira/rest/api/2/search?jql=project=%22")
            .append(name.toUpperCase())
            .append("%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR%22status%22")
            .append("=%22resolved%22)AND%22resolution%22=%22fixed%22")
            .append("&fields=key,resolutiondate,versions,created")
            .append("&maxResults=1000")
            .append("&startAt=");
        
        StringBuilder firstBuilder = origBuilder;
        String firstJsonBody = Http.get(firstBuilder.append("0").toString());

        JSONObject currentRootObject = new JSONObject(firstJsonBody);

        JiraTicketHeader ticketsHdr = new JiraTicketHeader(currentRootObject);
        int totalTickets = ticketsHdr.getTotal();
        if(totalTickets == 0) {
            return new JiraTicket[0];
        }

        JiraTicket[] tickets = new JiraTicket[totalTickets];

        int i = 0;
        do {
            if(i != 0 && i % 1000 == 0) {
                StringBuilder lastBuilder = origBuilder;
                lastBuilder.append(String.valueOf(i));
                String lastJsonBody = Http.get(lastBuilder.toString());

                currentRootObject = new JSONObject(lastJsonBody);
            }

            tickets[i] = new JiraTicket(currentRootObject);
        } while(++i < totalTickets);        

        return tickets;
    }
}
