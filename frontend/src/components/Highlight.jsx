import React from 'react';

/**
 * Reusable text highlight component supporting:
 * - "bar": a yellow (#ffe17c) rectangle behind the lower half of the word, rotated -2deg.
 * - "yellowText": the word in yellow on the surrounding background.
 * - "darkBox": the word in yellow text inside a charcoal rounded box.
 * - "outline": transparent fill with a 2px charcoal stroke.
 */
export default function Highlight({ variant = 'bar', children, className = '' }) {
  let variantClass = 'highlight-bar';

  switch (variant) {
    case 'yellowText':
      variantClass = 'highlight-yellowText';
      break;
    case 'darkBox':
      variantClass = 'highlight-darkBox';
      break;
    case 'outline':
      variantClass = 'highlight-outline';
      break;
    case 'bar':
    default:
      variantClass = 'highlight-bar';
      break;
  }

  return <span className={`${variantClass} ${className}`}>{children}</span>;
}
