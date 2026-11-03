import React, { createContext, useContext, useState } from 'react';

const FilterContext = createContext(null);

export const FilterProvider = ({ children }) => {
  const [category, setCategory] = useState('');
  const [selectedDrugId, setSelectedDrugId] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');
  const [dateRange, setDateRange] = useState({
    from: null,
    to: null,
  });

  const resetFilters = () => {
    setCategory('');
    setSelectedDrugId('');
    setSelectedStatus('');
    setDateRange({ from: null, to: null });
  };

  return (
    <FilterContext.Provider value={{
      category,
      setCategory,
      selectedDrugId,
      setSelectedDrugId,
      selectedStatus,
      setSelectedStatus,
      dateRange,
      setDateRange,
      resetFilters
    }}>
      {children}
    </FilterContext.Provider>
  );
};

export const useFilters = () => useContext(FilterContext);
