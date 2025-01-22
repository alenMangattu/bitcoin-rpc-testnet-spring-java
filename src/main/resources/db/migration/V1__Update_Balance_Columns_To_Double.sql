-- Update wallets table
ALTER TABLE wallets 
    ALTER COLUMN balance TYPE DOUBLE PRECISION USING balance::DOUBLE PRECISION;

-- Update transactions table
ALTER TABLE transactions 
    ALTER COLUMN amount TYPE DOUBLE PRECISION USING amount::DOUBLE PRECISION; 