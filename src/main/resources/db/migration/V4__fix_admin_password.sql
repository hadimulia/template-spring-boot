-- Fix admin password hash to actually match 'admin123'
UPDATE users SET password = '$2a$10$mL1Onwq9YlVNKfLngsghbujC4ueZSSaT8kJ/Urz/Z6O.5W1e1C4Kq'
WHERE username = 'admin';
