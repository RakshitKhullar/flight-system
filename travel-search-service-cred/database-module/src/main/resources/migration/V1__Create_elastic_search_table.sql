-- Create elastic_search table for flight search data
CREATE TABLE elastic_search (
    id BIGSERIAL PRIMARY KEY,
    flight_id VARCHAR(50) NOT NULL,
    source VARCHAR(100) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    flightdate DATE NOT NULL,
    maximumstops INTEGER CHECK (maximumstops IN (0, 1, 2)) NOT NULL DEFAULT 0,
    departner VARCHAR(100),
    seat_structure JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_elastic_search_flight_id ON elastic_search(flight_id);
CREATE INDEX idx_elastic_search_source_destination ON elastic_search(source, destination);
CREATE INDEX idx_elastic_search_flightdate ON elastic_search(flightdate);
CREATE INDEX idx_elastic_search_departner ON elastic_search(departner);
CREATE INDEX idx_elastic_search_maximumstops ON elastic_search(maximumstops);

-- Create a composite index for common search queries
CREATE INDEX idx_elastic_search_search_combo ON elastic_search(source, destination, flightdate, maximumstops);

-- Create trigger to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_elastic_search_updated_at 
    BEFORE UPDATE ON elastic_search 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();
