package ste.jirarest;

import org.json.JSONObject;

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

        private Fields(JSONObject o) {
            versions = (Version[]) Parsing.getArray(o.getJSONArray("versions"), Version.class);
        }

        public Version[] getVersions() {
            return versions;
        }
    }

    private final String expand;
    private final String id;
    private final String self;
    private final String key;
    private final Fields fields;
    private final String resolutionDate;
    private final String created;

    private JiraTicket(JSONObject o) {
        expand = o.getString("expand");
        id = o.getString("id");
        self = o.getString("self");
        key = o.getString("key");
        fields = new Fields(o.getJSONObject("fields"));
        resolutionDate = o.getString("resolutionDate");
        created = o.getString("created");
    }

    public String getCreated() {
        return created;
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

    public String getResolutionDate() {
        return resolutionDate;
    }

    public String getSelf() {
        return self;
    }
}
